package me.incognitojam.kahootj;

import me.incognitojam.kahootj.actionprovider.IActionProvider;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Headers.Builder;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class KahootClient implements Runnable {
    private static final String URL_BASE = "https://kahoot.it/cometd/";
    private static boolean debugMode = false;

    private String username;
    private String clientId;
    private String sessionToken;
    private String bayeuxCookie;

    private Game game;
    private IActionProvider actionProvider;

    public KahootClient(String username, IActionProvider actionProvider) {
        this.username = username;
        this.actionProvider = actionProvider;
    }

    @Override
    public void run() {
        if (game == null || game.gamepin == 0) throw new RuntimeException("No game set");

        try {
            // Enter game
            initialise();
            // Provide username
            login();
            // Begin game, using actionprovider
            play();
        } catch (Exception e) {
            System.out.println("KahootClient crashed with error: ");
            e.printStackTrace();
        }
    }

    private void play() throws IOException {
        while (game.active) {
            JSONObject postHeaders = new JSONObject();
            postHeaders.put("channel", "/meta/connect");
            postHeaders.put("connectionType", "long-polling");
            postHeaders.put("clientId", clientId);

            Headers headers = new Headers.Builder()
                    .add("Cookie", bayeuxCookie)
//                    .add("channel", "/meta/connect")
//                    .add("connectionType", "long-polling")
//                    .add("clientId", clientId)
                    .build();

            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/connect", postHeaders.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("response = " + responseString);
            JSONArray responseArray = new JSONArray(responseString);
            JSONObject responseObject = responseArray.getJSONObject(responseArray.length() - 1);
            JSONObject dataObject = responseArray.getJSONObject(0);
            boolean success = responseObject.getBoolean("successful");
            if (!success) {
                System.out.println("[LOGIN/FINISH] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
            if (responseString.contains("answerMap") && !responseString.contains("timeLeft")) {
                JSONObject data = dataObject.getJSONObject("data");
                JSONObject content = new JSONObject(data.getString("content").replace("\\", ""));
                JSONObject answers = content.getJSONObject("answerMap");
                if (answers.length() == 2) {
                    game.optionThreeValid = false;
                    game.optionFourValid = false;
                } else if (answers.length() == 3) {
                    game.optionThreeValid = true;
                    game.optionFourValid = false;
                } else {
                    game.optionThreeValid = true;
                    game.optionFourValid = true;
                }
                System.out.println("Answers: 0 through " + (answers.length() - 1));
                int ans = actionProvider.getChoice(game);
                game.previousAnswer = ans;
                int ra = answers.getInt(Integer.toString(ans));
                KahootClient.this.answerQuestion(ra);
//                System.out.print("Answer: ");
//                int ans = userInput.nextInt();
//                previousAnswer = ans;
//                int ra = answers.getInt(Integer.toString(ans));
//                Kahoot.this.answerQuestion(ra);
            } else if (responseString.contains("answerMap")) {
                System.out.println("Get ready, question is coming up!");
            }

            if (responseString.contains("primaryMessage")) {
                JSONObject data = dataObject.getJSONObject("data");
                JSONObject content = new JSONObject(data.getString("content").replace("\\", ""));
                String primaryMessage = content.getString("primaryMessage");
                System.out.println(primaryMessage);
            } else if (responseString.contains("isCorrect")) {
                JSONObject d = dataObject.getJSONObject("data");
                JSONObject c = new JSONObject(d.getString("content").replace("\\", ""));
                boolean correct = c.getBoolean("isCorrect");
                game.lastScore = c.getInt("points");
                game.totalScore = c.getInt("totalScore");
                game.currentRank = c.getInt("rank");
                Object rawNemesis = c.get("nemesis");
                if (rawNemesis == null) {
                    game.nemesis = "no one";
                } else {
                    game.nemesis = (String) rawNemesis;
                }
                System.out.println(correct ? "Correct!" : "Incorrect.");
                System.out.println("You got " + game.lastScore + " points from that question");
                System.out.println("You currently have " + game.totalScore + " points");
                System.out.println("You are in rank " + game.currentRank + ", behind " + game.nemesis);
            } else if (responseString.contains("quizId")) {
                JSONObject data = dataObject.getJSONObject("data");
                JSONObject content = new JSONObject(data.getString("content").replace("\\", ""));
                String quizId = content.getString("quizId");
                int playerCount = content.getInt("playerCount");
                System.out.println("This quiz's ID is " + quizId);
                System.out.println("Players in game: " + playerCount);
                game.active = false;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void answerQuestion(int answer) throws IOException {
        //String device = "{\"userAgent\": \"openhacky/1.7.7.1\", \"screen\": {\"width\": 1337, \"height\": 1337}}";
        JSONObject device = new JSONObject();
        device.put("userAgent", HTTPUtils.USER_AGENT);

        JSONObject screen = new JSONObject();
        screen.put("width", 1337);
        screen.put("height", 1337);

        device.put("screen", screen);

        //String content = "{\"choice\": " + ans + ", \"meta\": \"{\"lag\": 22, \"device\": \"" + device + "\"}\"}";

        JSONObject meta = new JSONObject();
        meta.put("lag", 22);
        meta.put("device", device);

        JSONObject content = new JSONObject();
        content.put("choice", answer);
        content.put("meta", meta);

        //String content = "{\"choice\":" + ans + ",\"meta\":{\"lag\":22,\"device\":{\"userAgent\":\"" + HTTP.uagent + "\",\"screen\":{\"width\":360,\"height\":640}}}}";

        JSONObject data = new JSONObject();
        data.put("id", 6);
        data.put("type", "message");
        data.put("gameid", game.gamepin);
        data.put("host", "kahoot.it");
        data.put("content", content.toString());
        //data.put("connectionType", "long-polling");

        JSONObject base = new JSONObject();
        base.put("clientId", clientId);
        base.put("channel", "/service/controller");
        base.put("data", data);

        //String data = "{\"id\": 6, \"type\": \"message\", \"gameid\": " + gameid + ", \"host\": \"kahoot.it\", \"content\": \"" + content + "\", \"channel\": \"/service/controller\", \"connectionType\", \"long-polling\", \"clientId\", \"" + client_id + "\"}";

        //System.out.println(base.toString());
        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, base.toString(), headers);
        Response response = call.execute();
        String responseString = response.body().string();
        if (KahootClient.isDebug())
            System.out.println("AQ = " + responseString);
        JSONArray r2 = new JSONArray(responseString);
        JSONObject r = r2.getJSONObject(r2.length() - 1);
        boolean success = r.getBoolean("successful");
        if (!success) {
            System.out.println("[QUESTION/ANSWER] Error connecting to server! Full server response below.");
            System.out.println(responseString);
        }
        game.questionAnswered = true;
    }

    private void disconnect() throws IOException {
        if (!isGameRunning()) {
            System.out.println("Attempted to disconnect when client not active!");
        }

        JSONObject c = new JSONObject();
        c.put("channel", "/meta/disconnect");
        c.put("clientId", clientId);

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/disconnect", c.toString(), headers);
        Response response = call.execute();
        String responseString = response.body().string();
        if (KahootClient.isDebug())
            System.out.println("D = " + responseString);
        JSONArray responseArray = new JSONArray(responseString);
        JSONObject responseObject = responseArray.getJSONObject(0);
        boolean success = responseObject.has("successful") && responseObject.getBoolean("successful");
        if (!success) {
            System.out.println("[DISCONNECT] Error connecting to server! Full server response below.");
            System.out.println(responseString);
        }

        game.active = false;
    }

    private void login() throws IOException {
        {
            JSONObject c = new JSONObject();
            c.put("channel", "/service/controller");
            c.put("clientId", clientId);
            //c.put("id", "62");

            JSONObject data = new JSONObject();
            data.put("type", "login");
            data.put("gameid", game.gamepin);
            data.put("host", "kahoot.it");
            data.put("name", username);

            c.put("data", data);

            //String d = "{\"clientId\":\"" + client_id + "\",\"data\":{\"gameid\":" + gameid + ",\"host\":\"kahoot.it\",\"name\":\"" + uname + "\",\"type\":\"login\"},\"channel\":\"/service/controller\"}";

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("L1 = " + responseString);
            JSONArray responseArray = new JSONArray(responseString);
            JSONObject responseObject = responseArray.getJSONObject(responseArray.length() - 1);
            boolean success = responseObject.getBoolean("successful");
            if (!success) {
                System.out.println("[LOGIN/BEGIN] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        {
            String headersData = "{\"clientId\":\"" + clientId + "\",\"channel\":\"/meta/connect\",\"connectionType\":\"long-polling\"}";
            Headers headers = new Builder()
                    .add("clientId", clientId)
                    .add("channel", "/meta/connect")
                    .add("connectionType", "long-polling")
                    .add("Cookie", bayeuxCookie)
                    .build();

            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/connect", headersData, headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("L2 = " + responseString);
            JSONArray r2 = new JSONArray(responseString);
            JSONObject r = r2.getJSONObject(r2.length() - 1);
            boolean success = r.getBoolean("successful");
            if (!success) {
                System.out.println("[LOGIN/FINISH] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        game.active = true;
    }

    private void initialise() throws IOException {
        sessionToken = SessionUtils.getSessionToken(game.gamepin);
        sessionToken = SessionUtils.decodeSessionToken(sessionToken);

        game.team = SessionUtils.getLastGameTeam();

        if (KahootClient.isDebug()) {
            System.out.println("stoken = " + sessionToken);
            System.out.println("gameid = " + game.gamepin);
        }

        // Stage ONE
        {
            JSONObject advice = new JSONObject();
            advice.put("timeout", 60000);
            advice.put("interval", 0);

            JSONObject handshakeData = new JSONObject();
            handshakeData.put("advice", advice.toString());
            handshakeData.put("version", "1.0");
            handshakeData.put("minimumVersion", "1.0");
            handshakeData.put("channel", "/meta/handshake");

            JSONArray supportedConnTypes = new JSONArray();
            //supportedConnTypes.put("websocket");
            supportedConnTypes.put("long-polling");

            handshakeData.put("supportedConnectionTypes", supportedConnTypes.toString());

            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/handshake", handshakeData.toString());
            Response response = call.execute();
            String responseString = response.body().string();
            Headers headers = response.headers();
            for (String headerKey : headers.names()) {
                if (headerKey.equalsIgnoreCase("Set-Cookie")) {
                    bayeuxCookie = headers.get(headerKey);
                    int sc = bayeuxCookie.lastIndexOf(';');
                    bayeuxCookie = bayeuxCookie.substring(0, sc);
                }
            }
            if (KahootClient.isDebug())
                System.out.println("1 = " + responseString);

            JSONArray responseJson = new JSONArray(responseString);
            JSONObject responseObject = responseJson.getJSONObject(0);
            clientId = responseObject.getString("clientId");

        }

        // STAGE TWO
        {
            JSONObject content = new JSONObject();
            content.put("channel", "/meta/unsubscribe");
            content.put("clientId", clientId);
            content.put("subscription", "/service/controller");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, content.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("stage 2 = " + response);

            JSONArray responseArray = new JSONArray(responseString);
            JSONObject responseObject = responseArray.getJSONObject(0);
            boolean success = responseObject.getBoolean("successful");
            if (!success) {
                System.out.println("[STAGE 2] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }

        }

        // STAGE THREE
        {
            JSONObject data = new JSONObject();
            data.put("channel", "/meta/connect");
            data.put("clientId", clientId);
            data.put("connectionType", "long-polling");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/connect", data.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("3 = " + responseString);
            JSONArray r2 = new JSONArray(responseString);
            JSONObject r = r2.getJSONObject(0);
            boolean success = r.getBoolean("successful");
            if (!success) {
                System.out.println("[STAGE 3] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        // STAGE FOUR - STATUS SERVICE
        {
            JSONObject c6 = new JSONObject();
            c6.put("channel", "/meta/subscribe");
            c6.put("clientId", clientId);
            //c6.put("connectionType", "long-polling");
            c6.put("subscription", "/service/status");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c6.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("4-1 = " + responseString);
            JSONArray r2 = new JSONArray(responseString);
            JSONObject r = r2.getJSONObject(0);
            boolean success = r.getBoolean("successful");
            if (!success) {
                System.out.println("[STAGE 4/SERVICE_STATUS] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        // STAGE FOUR - PLAYER SERVICE
        {
            JSONObject c7 = new JSONObject();
            c7.put("channel", "/meta/subscribe");
            c7.put("clientId", clientId);
            //c7.put("connectionType", "long-polling");
            c7.put("subscription", "/service/player");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c7.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("4-2 = " + responseString);
            JSONArray r2 = new JSONArray(responseString);
            JSONObject r = r2.getJSONObject(0);
            boolean success = r.getBoolean("successful");
            if (!success) {
                System.out.println("[STAGE 4/SERVICE_PLAYER] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        // STAGE FOUR - CONTROLLER SERVICE
        {
            JSONObject c8 = new JSONObject();
            c8.put("channel", "/meta/subscribe");
            c8.put("clientId", clientId);
            //c7.put("connectionType", "long-polling");
            c8.put("subscription", "/service/controller");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c8.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("4-3 = " + responseString);
            JSONArray r2 = new JSONArray(responseString);
            JSONObject r = r2.getJSONObject(0);
            boolean success = r.getBoolean("successful");
            if (!success) {
                System.out.println("[STAGE 4/SERVICE_CONTROLLER] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }
    }

    /**
     * Get the last answer submitted to a question by this Kahoot object.<br>
     * <br>
     * WARNING: This function is blocking, meaning it will halt execution (unless this object is on its own thread) until a question is answered. This function is useful for counting answers submitted by the rand() function.<br>
     * <br>
     * This function can carry a delay of 10 milliseconds before it returns, but Kahoot questions take more than 1000 milliseconds to load, so this shouldn't be a problem.
     *
     * @return last answer submitted
     * @throws InterruptedException if sleep is interrupted
     */
    public int getLastAnswerBlocking() throws InterruptedException {
        while (!game.questionAnswered) {
            // wait until qa is true
            Thread.sleep(10);
            if (!this.game.active)
                return -1; // halt if the game isn't active.
        }
        game.questionAnswered = false;
        return game.previousAnswer;
    }

    /**
     * Get the last answer submitted to a question by this Kahoot object.
     * <p>
     * WARNING: This function is non-blocking, meaning it can return the same answer for the same question twice. If you want a blocking function, check getLastAnswerBlocking()
     *
     * @return last answer submitted
     */
    public int getLastAnswer() {
        game.questionAnswered = false;
        return game.previousAnswer;
    }

    /**
     * Get the score from the last question answered
     *
     * @return score gained from last answered question
     */
    public int getLastScore() {
        return game.lastScore;
    }

    /**
     * Get your total score between all questions
     *
     * @return total score
     */
    public int getTotalScore() {
        return game.totalScore;
    }

    /**
     * Get your rank
     *
     * @return your rank
     */
    public int getRank() {
        return game.currentRank;
    }

    /**
     * Get this Kahoot object's nemesis, or the player directly ahead of it.
     *
     * @return the player directly ahead of this Kahoot object
     */
    public String getNemesis() {
        return game.nemesis;
    }

    /**
     * Check whether this game is a team game or not
     *
     * @return true if team game, false if classic PvP
     */
    public boolean isTeamGame() {
        return game != null && game.team;
    }

    /**
     * Check if the game is running (in progress)
     *
     * @return true if the game is running, otherwise false
     */
    public boolean isGameRunning() {
        return game != null && game.active;
    }

    public Game getGame() {
        return game;
    }

    public boolean setGame(int gamepin) {
        if (game != null && game.active) {
            System.out.println("Cannot update gamepin when game is active!");
            return false;
        }

        game = new Game(gamepin);
        return true;
    }

    public static void setDebug(boolean debugMode) {
        KahootClient.debugMode = debugMode;
    }

    public static boolean isDebug() {
        return debugMode;
    }

    @Override
    public String toString() {
        return "KahootClient{" +
                "username='" + username + '\'' +
                ", clientId='" + clientId + '\'' +
                ", game=" + game +
                '}';
    }

}
