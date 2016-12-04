package me.incognitojam.kahootj;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.incognitojam.kahootj.actionprovider.IActionProvider;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Headers.Builder;
import okhttp3.Response;

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
            JsonObject postHeaders = new JsonObject();
            postHeaders.addProperty("channel", "/meta/connect");
            postHeaders.addProperty("connectionType", "long-polling");
            postHeaders.addProperty("clientId", clientId);

            Headers headers = new Headers.Builder()
                    .add("Cookie", bayeuxCookie)
//                    .add("channel", "/meta/connect")
//                    .add("connectionType", "long-polling")
//                    .add("clientId", clientId)
                    .build();

            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/connect", postHeaders.toString(), headers);
            Response response;
            try {
                response = call.execute();
            } catch (Exception e) {
                response = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/connect", postHeaders.toString(), headers).execute();
            }
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("response = " + responseString);
            JsonArray responseArray = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject responseObject = responseArray.get(responseArray.size() - 1).getAsJsonObject();
            JsonObject dataObject = responseArray.get(0).getAsJsonObject();
            boolean success = responseObject.get("successful").getAsBoolean();
            if (!success) {
                System.out.println("[LOGIN/FINISH] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
            if (responseString.contains("answerMap") && !responseString.contains("timeLeft")) {
                JsonObject data = dataObject.get("data").getAsJsonObject();
                JsonObject content = new JsonParser().parse(data.get("content").getAsString().replace("\\", "")).getAsJsonObject();
                JsonObject answers = content.get("answerMap").getAsJsonObject();
                if (answers.size() == 2) {
                    game.optionThreeValid = false;
                    game.optionFourValid = false;
                } else if (answers.size() == 3) {
                    game.optionThreeValid = true;
                    game.optionFourValid = false;
                } else {
                    game.optionThreeValid = true;
                    game.optionFourValid = true;
                }
                System.out.println("Answers: 0 through " + (answers.size() - 1));
                int ans = actionProvider.getChoice(game);
                game.previousAnswer = ans;
                int ra = answers.get(Integer.toString(ans)).getAsInt();
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
                JsonObject data = dataObject.get("data").getAsJsonObject();
                JsonObject content = new JsonParser().parse(data.get("content").getAsString().replace("\\", "")).getAsJsonObject();
                String primaryMessage = content.get("primaryMessage").getAsString();
                System.out.println(primaryMessage);
            } else if (responseString.contains("isCorrect")) {
                JsonObject d = dataObject.get("data").getAsJsonObject();
                JsonObject c = new JsonParser().parse(d.get("content").getAsString().replace("\\", "")).getAsJsonObject();
                boolean correct = c.get("isCorrect").getAsBoolean();
                game.lastScore = c.get("points").getAsInt();
                game.totalScore = c.get("totalScore").getAsInt();
                game.currentRank = c.get("rank").getAsInt();
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
                JsonObject data = dataObject.get("data").getAsJsonObject();
                JsonObject content = new JsonParser().parse(data.get("content").getAsString().replace("\\", "")).getAsJsonObject();
                String quizId = content.get("quizId").getAsString();
                int playerCount = content.get("playerCount").getAsInt();
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
        JsonObject device = new JsonObject();
        device.addProperty("userAgent", HTTPUtils.USER_AGENT);

        JsonObject screen = new JsonObject();
        screen.addProperty("width", 1337);
        screen.addProperty("height", 1337);
        
        device.add("screen", screen);

        //String content = "{\"choice\": " + ans + ", \"meta\": \"{\"lag\": 22, \"device\": \"" + device + "\"}\"}";

        JsonObject meta = new JsonObject();
        meta.addProperty("lag", 22);
        meta.add("device", device);

        JsonObject content = new JsonObject();
        content.addProperty("choice", answer);
        content.add("meta", meta);

        //String content = "{\"choice\":" + ans + ",\"meta\":{\"lag\":22,\"device\":{\"userAgent\":\"" + HTTP.uagent + "\",\"screen\":{\"width\":360,\"height\":640}}}}";

        JsonObject data = new JsonObject();
        data.addProperty("id", 6);
        data.addProperty("type", "message");
        data.addProperty("gameid", game.gamepin);
        data.addProperty("host", "kahoot.it");
        data.addProperty("content", content.toString());
        //data.put("connectionType", "long-polling");

        JsonObject base = new JsonObject();
        base.addProperty("clientId", clientId);
        base.addProperty("channel", "/service/controller");
        base.add("data", data);

        //String data = "{\"id\": 6, \"type\": \"message\", \"gameid\": " + gameid + ", \"host\": \"kahoot.it\", \"content\": \"" + content + "\", \"channel\": \"/service/controller\", \"connectionType\", \"long-polling\", \"clientId\", \"" + client_id + "\"}";

        //System.out.println(base.toString());
        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, base.toString(), headers);
        Response response = call.execute();
        String responseString = response.body().string();
        if (KahootClient.isDebug())
            System.out.println("AQ = " + responseString);
        JsonArray r2 = new JsonParser().parse(responseString).getAsJsonArray();
        JsonObject r = r2.get(r2.size() - 1).getAsJsonObject();
        boolean success = r.get("successful").getAsBoolean();
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

        JsonObject c = new JsonObject();
        c.addProperty("channel", "/meta/disconnect");
        c.addProperty("clientId", clientId);

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/disconnect", c.toString(), headers);
        Response response = call.execute();
        String responseString = response.body().string();
        if (KahootClient.isDebug())
            System.out.println("D = " + responseString);
        JsonArray responseArray = new JsonParser().parse(responseString).getAsJsonArray();
        JsonObject responseObject = responseArray.get(0).getAsJsonObject();
        boolean success = responseObject.has("successful") && responseObject.get("successful").getAsBoolean();
        if (!success) {
            System.out.println("[DISCONNECT] Error connecting to server! Full server response below.");
            System.out.println(responseString);
        }

        game.active = false;
    }

    private void login() throws IOException {
        {
            JsonObject c = new JsonObject();
            c.addProperty("channel", "/service/controller");
            c.addProperty("clientId", clientId);
            //c.put("id", "62");

            JsonObject data = new JsonObject();
            data.addProperty("type", "login");
            data.addProperty("gameid", game.gamepin);
            data.addProperty("host", "kahoot.it");
            data.addProperty("name", username);

            c.add("data", data);

            //String d = "{\"clientId\":\"" + client_id + "\",\"data\":{\"gameid\":" + gameid + ",\"host\":\"kahoot.it\",\"name\":\"" + uname + "\",\"type\":\"login\"},\"channel\":\"/service/controller\"}";

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("L1 = " + responseString);
            JsonArray responseArray = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject responseObject = responseArray.get(responseArray.size() - 1).getAsJsonObject();
            boolean success = responseObject.get("successful").getAsBoolean();
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
            JsonArray r2 = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject r = r2.get(r2.size() - 1).getAsJsonObject();
            boolean success = r.get("successful").getAsBoolean();
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
            JsonObject advice = new JsonObject();
            advice.addProperty("timeout", 60000);
            advice.addProperty("interval", 0);

            JsonObject handshakeData = new JsonObject();
            handshakeData.addProperty("advice", advice.toString());
            handshakeData.addProperty("version", "1.0");
            handshakeData.addProperty("minimumVersion", "1.0");
            handshakeData.addProperty("channel", "/meta/handshake");

            JsonArray supportedConnTypes = new JsonArray();
            //supportedConnTypes.put("websocket");
            supportedConnTypes.add("long-polling");

            handshakeData.addProperty("supportedConnectionTypes", supportedConnTypes.toString());

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

            System.out.println(responseString);
            JsonArray responseJson = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject responseObject = responseJson.get(0).getAsJsonObject();
            clientId = responseObject.get("clientId").getAsString();

        }

        // STAGE TWO
        {
            JsonObject content = new JsonObject();
            content.addProperty("channel", "/meta/unsubscribe");
            content.addProperty("clientId", clientId);
            content.addProperty("subscription", "/service/controller");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, content.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("stage 2 = " + response);

            JsonArray responseArray = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject responseObject = responseArray.get(0).getAsJsonObject();
            boolean success = responseObject.get("successful").getAsBoolean();
            if (!success) {
                System.out.println("[STAGE 2] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }

        }

        // STAGE THREE
        {
            JsonObject data = new JsonObject();
            data.addProperty("channel", "/meta/connect");
            data.addProperty("clientId", clientId);
            data.addProperty("connectionType", "long-polling");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken + "/connect", data.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("3 = " + responseString);
            JsonArray r2 = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject r = r2.get(0).getAsJsonObject();
            boolean success = r.get("successful").getAsBoolean();
            if (!success) {
                System.out.println("[STAGE 3] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        // STAGE FOUR - STATUS SERVICE
        {
            JsonObject c6 = new JsonObject();
            c6.addProperty("channel", "/meta/subscribe");
            c6.addProperty("clientId", clientId);
            //c6.addProperty("connectionType", "long-polling");
            c6.addProperty("subscription", "/service/status");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c6.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("4-1 = " + responseString);
            JsonArray r2 = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject r = r2.get(0).getAsJsonObject();
            boolean success = r.get("successful").getAsBoolean();
            if (!success) {
                System.out.println("[STAGE 4/SERVICE_STATUS] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        // STAGE FOUR - PLAYER SERVICE
        {
            JsonObject c7 = new JsonObject();
            c7.addProperty("channel", "/meta/subscribe");
            c7.addProperty("clientId", clientId);
            //c7.addProperty("connectionType", "long-polling");
            c7.addProperty("subscription", "/service/player");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c7.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("4-2 = " + responseString);
            JsonArray r2 = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject r = r2.get(0).getAsJsonObject();
            boolean success = r.get("successful").getAsBoolean();
            if (!success) {
                System.out.println("[STAGE 4/SERVICE_PLAYER] Error connecting to server! Full server response below.");
                System.out.println(responseString);
            }
        }

        // STAGE FOUR - CONTROLLER SERVICE
        {
            JsonObject c8 = new JsonObject();
            c8.addProperty("channel", "/meta/subscribe");
            c8.addProperty("clientId", clientId);
            //c7.addProperty("connectionType", "long-polling");
            c8.addProperty("subscription", "/service/controller");

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            Call call = HTTPUtils.POST(URL_BASE + game.gamepin + "/" + sessionToken, c8.toString(), headers);
            Response response = call.execute();
            String responseString = response.body().string();
            if (KahootClient.isDebug())
                System.out.println("4-3 = " + responseString);
            JsonArray r2 = new JsonParser().parse(responseString).getAsJsonArray();
            JsonObject r = r2.get(0).getAsJsonObject();
            boolean success = r.get("successful").getAsBoolean();
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
