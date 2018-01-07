package me.incognitojam.kahootj.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.incognitojam.kahootj.KahootClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;

public class SessionUtils {

    public interface SessionCallback<T> {

        void onSuccess(T object);

        void onFailure(Exception exception);

    }

    private static boolean wasLastGameTeam = false;

    private static String challengeSolution; // last challenge solution

    public static boolean getLastGameTeam() {
        return wasLastGameTeam;
    }

    private static String solveChallenge(String challenge) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");

        System.out.println("Challenge: " + challenge);

        String message;
        double offset;

        {
            int firstIndex = challenge.indexOf("'");
            int lastIndex = challenge.lastIndexOf("'");
            message = challenge.substring(firstIndex + 1, lastIndex);

            System.out.println("Message: " + message);
        }

        {
            int firstIndex = challenge.indexOf("=");
            int lastIndex = challenge.indexOf(";", firstIndex);
            String offsetCalculation = challenge.substring(firstIndex + 1, lastIndex);
            try {
                offset = Double.valueOf(engine.eval(offsetCalculation).toString());
            } catch (ScriptException e) {
                e.printStackTrace();
                offset = 0;
            }

            System.out.println("Offset: " + offset);
        }

        int length = message.length();
        char[] array = new char[length];

        for (int position = 0; position < length; position++) {
            char c = message.charAt(position);
            char c2 = (char) ((((c * position) + offset) % 77) + 48);
            array[position] = c2;
        }

        String decoded = new String(array);
        System.out.println("Decoded message: " + decoded);

        return decoded;
    }

    /**
     * Check if a game PIN is valid.
     *
     * @param gamePin The game PIN to check
     * @return true if game PIN is valid, false if game PIN is invalid or an exception was thrown.
     */
    public static boolean checkPINValidity(int gamePin) {
        return isResponseValid(HTTPUtils.GET_RESPONSE("https://kahoot.it/reserve/session/" + gamePin + "/?" + System.currentTimeMillis()));
    }

    public static void checkPINValidity(int gamePin, SessionCallback<Boolean> callback) {
        Call call = HTTPUtils.GET("https://kahoot.it/reserve/session/" + gamePin + "/?" + System.currentTimeMillis());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
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
    public static String decodeSessionToken(String encoded) {
        byte[] rawToken = Base64Utils.decode(encoded);
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
     * @param gamePin The game PIN to retrieve a session token for
     * @return The encoded session token
     */
    public static String getSessionToken(int gamePin) throws IOException {
        Response response = HTTPUtils.GET_RESPONSE("https://kahoot.it/reserve/session/" + gamePin + "/?" + System.currentTimeMillis());
        if (response == null) {
            System.out.println("Response is null");
            return null;
        }
        Headers headers = response.headers();
        for (String key : headers.names()) {
            if (key.equalsIgnoreCase("x-kahoot-session-token")) {
                String responseString = response.body().string();

                if (KahootClient.isDebug()) {
                    KahootClient.log("SESSION = " + headers.get(key));
                    KahootClient.log("SESSION REQUEST RESPONSE BODY = " + responseString);
                }

                wasLastGameTeam = responseString.contains("team");
                if (responseString.toLowerCase().contains("challenge")) {
                    JsonObject jsonObject = new JsonParser().parse(responseString).getAsJsonObject();
                    String challenge = jsonObject.get("challenge").getAsString();
                    challengeSolution = solveChallenge(challenge);
                }
                response.close();
                return headers.get(key);
            }
        }
        response.close();
        KahootClient.log("getSessionToken() null");
        return null;
    }

}
