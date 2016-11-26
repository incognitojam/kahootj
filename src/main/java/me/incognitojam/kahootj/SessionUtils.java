package me.incognitojam.kahootj;

import okhttp3.Headers;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.Base64;
import java.util.regex.Pattern;

public class SessionUtils {

    private static boolean wasLastGameTeam = false;

    private static long challengeSolution = 0; // last challenge solution

    static boolean getLastGameTeam() {
        return wasLastGameTeam;
    }

    private static long solveChallenge(String challenge) {
        challenge = challenge.replace("  ", " ");
        String[] challengeArray;

        long solution;

        // Numbers occur on each even index of the array such as 0, 2, 4, and so on
        // Operators occur on each odd index of the array such as 1, 3, 5, and so on

        if (Pattern.matches("^([0-9]*) \\* \\(([0-9]*) \\+ ([0-9]*)\\)$", challenge)) {
            challenge = challenge.replace("(", "").replace(")", "");
            challengeArray = challenge.split(" ");
            long num1 = Integer.parseInt(challengeArray[0]);
            long num2 = Integer.parseInt(challengeArray[2]);
            long num3 = Integer.parseInt(challengeArray[4]);

            solution = num1 * (num2 + num3);
        } else if (Pattern.matches("^\\(([0-9]*) \\+ ([0-9]*)\\) \\* ([0-9]*)$", challenge)) {
            challenge = challenge.replace("(", "").replace(")", "");
            challengeArray = challenge.split(" ");
            long num1 = Integer.parseInt(challengeArray[0]);
            long num2 = Integer.parseInt(challengeArray[2]);
            long num3 = Integer.parseInt(challengeArray[4]);

            solution = (num1 + num2) * num3;
        } else if (Pattern.matches("^([0-9]*) - \\(([0-9]*) \\* ([0-9]*)\\)$", challenge)) {
            challenge = challenge.replace("(", "").replace(")", "");
            challengeArray = challenge.split(" ");
            long num1 = Integer.parseInt(challengeArray[0]);
            long num2 = Integer.parseInt(challengeArray[2]);
            long num3 = Integer.parseInt(challengeArray[4]);

            solution = num1 - (num2 * num3);
        } else if (Pattern.matches("^\\(([0-9]*) \\+ ([0-9]*)\\) \\* \\(([0-9]*) \\* ([0-9]*)\\)$", challenge)) {
            challenge = challenge.replace("(", "").replace(")", "");
            challengeArray = challenge.split(" ");
            long num1 = Integer.parseInt(challengeArray[0]);
            long num2 = Integer.parseInt(challengeArray[2]);
            long num3 = Integer.parseInt(challengeArray[4]);
            long num4 = Integer.parseInt(challengeArray[6]);

            solution = (num1 + num2) * (num3 * num4);
        } else {
            challenge = challenge.replace("(", "").replace(")", "");
            challengeArray = challenge.split(" ");
            solution = -1;
            System.out.println("An unknown challenge was returned. Please report this to the developers.");
            for (int i = 0; i < challengeArray.length; i++) {
                System.out.println("challengeArray[" + i + "] = '" + challengeArray[i] + "'");
            }
        }

        if (Kahoot.isDebug()) {
            for (int i = 0; i < challengeArray.length; i++) {
                System.out.println("challengeArray[" + i + "] = '" + challengeArray[i] + "'");
            }
            System.out.println("CHALLENGE SOLUTION = " + solution);
        }

        return solution;
    }

    /**
     * Check if a game PIN is valid.
     *
     * @param gamepin The game PIN to check
     * @return true if game PIN is valid, false if game PIN is invalid or an exception was thrown.
     */
    public static boolean checkPINValidity(int gamepin) {
        Response response = HTTPUtils.GET("https://kahoot.it/reserve/session/" + gamepin + "/?" + System.currentTimeMillis());
        return response != null && response.code() == 200;
    }

    /**
     * Uses the last challenge solution to decode the session token.
     *
     * @param encoded The encoded session token
     * @return The decoded, usable session token
     */
    public static String decodeSessionToken(String encoded) {
        byte[] rawToken = Base64.getDecoder().decode(encoded);
        byte[] challengeBytes = Long.toString(challengeSolution).getBytes();

        for (int i = 0; i < rawToken.length; i++) {
            rawToken[i] ^= challengeBytes[i % challengeBytes.length];
        }

        return new String(rawToken);
    }

    /**
     * Retrieve a session token.<br>
     * Note that this function doesn't return the session token in a usable state.<br>
     * The session token must be decoded using decodeSessionToken() before it can be used.
     *
     * @param gamepin The game PIN to retrieve a session token for
     * @return The encoded session token
     */
    public static String getSessionToken(int gamepin) {
        Response response = HTTPUtils.GET("https://kahoot.it/reserve/session/" + gamepin + "/?" + System.currentTimeMillis());
        if (response == null) {
            System.out.println("Response is null");
            return null;
        }
        Headers headers = response.headers();
        try {
            for (String key : headers.names()) {
                if (key.equalsIgnoreCase("x-kahoot-session-token")) {
                    String responseString = response.body().string();

                    if (Kahoot.isDebug()) {
                        System.out.println("SESSION = " + headers.get(key));
                        System.out.println("SESSION REQUEST RESPONSE BODY = " + responseString);
                    }

                    wasLastGameTeam = responseString.contains("team");
                    if (responseString.toLowerCase().contains("challenge")) {
                        JSONObject jsonObject = new JSONObject(responseString);
                        String challenge = jsonObject.getString("challenge");
                        challengeSolution = solveChallenge(challenge);
                    }
                    return headers.get(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("getSessionToken() null");
        return null;
    }

}
