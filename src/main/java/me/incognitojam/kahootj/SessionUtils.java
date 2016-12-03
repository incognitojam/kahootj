package me.incognitojam.kahootj;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Base64;

public class SessionUtils {

    public interface SessionCallback<T> {

        void onSuccess(T object);

        void onFailure(Exception exception);

    }

    private static boolean wasLastGameTeam = false;

    private static String challengeSolution; // last challenge solution

    static boolean getLastGameTeam() {
        return wasLastGameTeam;
    }

    private static String solveChallenge(String challenge) throws IOException {
        String urlEncodedChallenge = URLEncoder.encode(challenge, "UTF-8").replace("*", "%2A");
        Call call = HTTPUtils.GET("http://safeval.pw/eval?code=" + urlEncodedChallenge);
        Response response = call.execute();
        return response.body().string();
    }

    /**
     * Check if a game PIN is valid.
     *
     * @param gamepin The game PIN to check
     * @return true if game PIN is valid, false if game PIN is invalid or an exception was thrown.
     */
    public static boolean checkPINValidity(int gamepin) throws IOException {
        return isResponseValid(HTTPUtils.GET_RESPONSE("https://kahoot.it/reserve/session/" + gamepin + "/?" + System.currentTimeMillis()));
    }

    public static void checkPINValidity(int gamepin, SessionCallback<Boolean> callback) {
        Call call = HTTPUtils.GET("https://kahoot.it/reserve/session/" + gamepin + "/?" + System.currentTimeMillis());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onSuccess(isResponseValid(response));
            }
        });
    }

    public static boolean isResponseValid(Response response) {
        return response != null && response.code() == 200;
    }

    /**
     * Uses the last challenge solution to decode the session token.
     * @param encoded The encoded session token
     * @return The decoded, usable session token
     */
    static String decodeSessionToken(String encoded) {
        byte[] rawToken = Base64.getDecoder().decode(encoded);
        byte[] challengeBytes = challengeSolution.getBytes(Charset.forName("ASCII"));

        for (int i = 0; i < rawToken.length; i++) {
            rawToken[i] ^= challengeBytes[i % challengeBytes.length];
        }

        return new String(rawToken, Charset.forName("ASCII"));
    }

    /**
     * Retrieve a session token.<br>
     * Note that this function doesn't return the session token in a usable state.<br>
     * The session token must be decoded using decodeSessionToken() before it can be used.
     *
     * @param gamepin The game PIN to retrieve a session token for
     * @return The encoded session token
     */
    public static String getSessionToken(int gamepin) throws IOException {
        Response response = HTTPUtils.GET_RESPONSE("https://kahoot.it/reserve/session/" + gamepin + "/?" + System.currentTimeMillis());
        if (response == null) {
            System.out.println("Response is null");
            return null;
        }
        Headers headers = response.headers();
        for (String key : headers.names()) {
            if (key.equalsIgnoreCase("x-kahoot-session-token")) {
                String responseString = response.body().string();

                if (KahootClient.isDebug()) {
                    System.out.println("SESSION = " + headers.get(key));
                    System.out.println("SESSION REQUEST RESPONSE BODY = " + responseString);
                }

                wasLastGameTeam = responseString.contains("team");
                if (responseString.toLowerCase().contains("challenge")) {
                    JSONObject jsonObject = new JSONObject(responseString);
                    System.out.println(jsonObject);
                    String challenge = jsonObject.getString("challenge");
                    System.out.println(challenge);
                    challengeSolution = solveChallenge(challenge);
                }
                return headers.get(key);
            }
        }
        System.out.println("getSessionToken() null");
        return null;
    }

}
