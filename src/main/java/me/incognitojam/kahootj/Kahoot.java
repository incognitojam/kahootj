package me.incognitojam.kahootj;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Headers.Builder;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class Kahoot extends Thread {

    private final String URL_BASE = "https://kahoot.it/cometd/";
    private String username; // This Kahoot object's username
    private String clientId; // Unique client ID assigned to Kahoot clients
    private String sessionToken; // This Kahoot object's session token
    private String bayeuxCookie;
    private boolean active = false; // Whether this Kahoot object is engaged in an active game or not
    private int gamePin; // The game pin
    private Scanner userInput; // A scanner hopefully scanning System.in
    private int gamemode; // This Kahoot object's operation mode. 1 = play normally, 2 = auto answer questions randomly
    private int previousAnswer; // The last answer submitted to the game
    private int score; // Score from last question
    private int totalScore; // Total score
    private int rank; // This Kahoot object's rank
    private String nemesis; // The person directly ahead of us. If there is no one ahead of us (1st place) this will be set to "no one"
    private boolean questionAnswered; // Question answered? prevents duplicate returns on getLastAnswerBlocking
    private boolean optionTwoValid = false; // Was 2 a valid answer on the last question?
    private boolean optionThreeValid = false; // Was 3 a valid answer on the last question?
    private boolean isTeam = false; // Is this a team game or classic PvP?

    private static boolean debuggingMode = false; //Connection debug mode, not useful to regular users

    /**
     * Construct a new Kahoot object. The newly constructed object can be thought of as a computer player.<br>
     * This Kahoot object can act as a bot that automatically answers questions randomly.<br>
     * It can also act as a regular player, depending on input from the user.
     *
     * @param username  the username that this Kahoot object will use to connect to the game
     * @param gamePin   the Kahoot.it game PIN
     * @param userInput a Scanner scanning System.in, if this parameter is not scanning System.in, expect bugs and even crashes.
     * @param gamemode  the gamemode. 1 = play normally, 2 = auto answer questions randomly, anything else is invalid
     * @param active    whether the object should instantly be active. If unsure, set to false.
     */
    public Kahoot(String username, int gamePin, Scanner userInput, int gamemode, boolean active) {
        this.username = username;
        this.userInput = userInput;
        this.gamemode = gamemode;
        this.active = active;
        this.gamePin = gamePin;
    }

    /**
     * Start this Kahoot object in a new thread, making concurrency easier.
     */
    public void run() {
        if (gamemode == 1) {
            // Auto initialization is unnecessary as this Kahoot object is registered as a regular user.
            this.login();
            System.out.println("You should be in game, see your name on screen?");
            this.play();
        } else if (gamemode == 2) {
            this.initialize();
            this.login();
            this.rand();
        }
        // Any other gamemode is invalid, so disconnect right after login.
        this.disconnect();
    }

    public static boolean isDebug() {
        return debuggingMode;
    }

    public static void setDebug(boolean debuggingMode) {
        Kahoot.debuggingMode = debuggingMode;
    }

    /**
     * Check if answer 2 was a valid answer on the last question.
     *
     * @return true if answer 2 was valid, otherwise false
     */
    public boolean wasLastQuestionAnswer2Valid() {
        return optionTwoValid;
    }

    /**
     * Check if answer 3 was a valid answer on the last question.
     *
     * @return true if answer 3 was valid, otherwise false
     */
    public boolean wasLastQuestionAnswer3Valid() {
        return optionThreeValid;
    }

    /**
     * Check if the game is running (in progress)
     *
     * @return true if the game is running, otherwise false
     */
    public boolean gameRunning() {
        return active;
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
        while (!questionAnswered) {
            // wait until qa is true
            Thread.sleep(10);
            if (!this.active)
                return -1; // halt if the game isn't active.
        }
        questionAnswered = false;
        return previousAnswer;
    }

    /**
     * Get the last answer submitted to a question by this Kahoot object.
     * <p>
     * WARNING: This function is non-blocking, meaning it can return the same answer for the same question twice. If you want a blocking function, check getLastAnswerBlocking()
     *
     * @return last answer submitted
     */
    public int getLastAnswer() {
        questionAnswered = false;
        return previousAnswer;
    }

    /**
     * Get the score from the last question answered
     *
     * @return score gained from last answered question
     */
    public int getLastScore() {
        return score;
    }

    /**
     * Get your total score between all questions
     *
     * @return total score
     */
    public int getTotalScore() {
        return totalScore;
    }

    /**
     * Get your rank
     *
     * @return your rank
     */
    public int getRank() {
        return rank;
    }

    /**
     * Get this Kahoot object's nemesis, or the player directly ahead of it.
     *
     * @return the player directly ahead of this Kahoot object
     */
    public String getNemesis() {
        return nemesis;
    }

    /**
     * Check whether this game is a team game or not
     *
     * @return true if team game, false if classic PvP
     */
    public boolean isTeamGame() {
        return isTeam;
    }

    /**
     * Initialize this Kahoot object and connect to the server.<br>
     * This function does the same thing as if you were to enter the game PIN on the Kahoot website, but it does not log you in.<br>
     * <br>
     * This function must be called regardless of whether the bot will be allowed to manage itself or not. Check the run() documentation for more information.
     */
    public void initialize() {
        sessionToken = SessionUtils.getSessionToken(gamePin);
        sessionToken = SessionUtils.decodeSessionToken(sessionToken);

        isTeam = SessionUtils.getLastGameTeam();

        if (Kahoot.isDebug()) {
            System.out.println("stoken = " + sessionToken);
            System.out.println("gameid = " + gamePin);
        }

        initStageOne();
    }

    private void initStageOne() {
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

        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken + "/handshake", handshakeData.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    Headers headers = response.headers();
                    for (String headerKey : headers.names()) {
                        if (headerKey.equalsIgnoreCase("Set-Cookie")) {
                            bayeuxCookie = headers.get(headerKey);
                            int sc = bayeuxCookie.lastIndexOf(';');
                            bayeuxCookie = bayeuxCookie.substring(0, sc);
                        }
                    }
                    if (Kahoot.isDebug())
                        System.out.println("1 = " + responseString);

                    JSONArray responseJson = new JSONArray(responseString);
                    JSONObject responseObject = responseJson.getJSONObject(0);
                    clientId = responseObject.getString("clientId");
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                initStageTwo();
            }
        });
    }

    private void initStageTwo() {
        // BEGIN STAGE 2
        JSONObject content = new JSONObject();
        content.put("channel", "/meta/unsubscribe");
        content.put("clientId", clientId);
        content.put("subscription", "/service/controller");

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken, content.toString(), headers, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    if (Kahoot.isDebug())
                        System.out.println("stage 2 = " + response);

                    JSONArray responseArray = new JSONArray(responseString);
                    JSONObject responseObject = responseArray.getJSONObject(0);
                    boolean success = responseObject.getBoolean("successful");
                    if (!success) {
                        System.out.println("[STAGE 2] Error connecting to server! Full server response below.");
                        System.out.println(responseString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                initStageThree();
            }
        });
    }

    private void initStageThree() {
        // BEGIN STAGE 3
        JSONObject data = new JSONObject();
        data.put("channel", "/meta/connect");
        data.put("clientId", clientId);
        data.put("connectionType", "long-polling");

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken + "/connect", data.toString(), headers, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    if (Kahoot.isDebug())
                        System.out.println("3 = " + responseString);
                    JSONArray r2 = new JSONArray(responseString);
                    JSONObject r = r2.getJSONObject(0);
                    boolean success = r.getBoolean("successful");
                    if (!success) {
                        System.out.println("[STAGE 3] Error connecting to server! Full server response below.");
                        System.out.println(responseString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                initStageFourA();
            }
        });

    }

    private void initStageFourA() {
        // STAGE FOUR - SERVICE STATUS
        JSONObject c6 = new JSONObject();
        c6.put("channel", "/meta/subscribe");
        c6.put("clientId", clientId);
        //c6.put("connectionType", "long-polling");
        c6.put("subscription", "/service/status");

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken, c6.toString(), headers, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    if (Kahoot.isDebug())
                        System.out.println("4-1 = " + responseString);
                    JSONArray r2 = new JSONArray(responseString);
                    JSONObject r = r2.getJSONObject(0);
                    boolean success = r.getBoolean("successful");
                    if (!success) {
                        System.out.println("[STAGE 4/SERVICE_STATUS] Error connecting to server! Full server response below.");
                        System.out.println(responseString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                initStageFourB();
            }
        });
    }

    private void initStageFourB() {
        // SERVICE PLAYER
        JSONObject c7 = new JSONObject();
        c7.put("channel", "/meta/subscribe");
        c7.put("clientId", clientId);
        //c7.put("connectionType", "long-polling");
        c7.put("subscription", "/service/player");

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken, c7.toString(), headers, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    if (Kahoot.isDebug())
                        System.out.println("4-2 = " + responseString);
                    JSONArray r2 = new JSONArray(responseString);
                    JSONObject r = r2.getJSONObject(0);
                    boolean success = r.getBoolean("successful");
                    if (!success) {
                        System.out.println("[STAGE 4/SERVICE_PLAYER] Error connecting to server! Full server response below.");
                        System.out.println(responseString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                initStageFourC();
            }
        });
    }

    private void initStageFourC() {
        // SERVICE CONTROLLER
        JSONObject c8 = new JSONObject();
        c8.put("channel", "/meta/subscribe");
        c8.put("clientId", clientId);
        //c7.put("connectionType", "long-polling");
        c8.put("subscription", "/service/controller");

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken, c8.toString(), headers, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    if (Kahoot.isDebug())
                        System.out.println("4-3 = " + responseString);
                    JSONArray r2 = new JSONArray(responseString);
                    JSONObject r = r2.getJSONObject(0);
                    boolean success = r.getBoolean("successful");
                    if (!success) {
                        System.out.println("[STAGE 4/SERVICE_CONTROLLER] Error connecting to server! Full server response below.");
                        System.out.println(responseString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Login to the Kahoot game.<br>
     * This function is does the same thing as if you were to enter your nickname in the second screen you would see in your browser.
     */
    public void login() {
        {
            JSONObject c = new JSONObject();
            c.put("channel", "/service/controller");
            c.put("clientId", clientId);
            //c.put("id", "62");

            JSONObject data = new JSONObject();
            data.put("type", "login");
            data.put("gameid", gamePin);
            data.put("host", "kahoot.it");
            data.put("name", username);

            c.put("data", data);

            //String d = "{\"clientId\":\"" + client_id + "\",\"data\":{\"gameid\":" + gameid + ",\"host\":\"kahoot.it\",\"name\":\"" + uname + "\",\"type\":\"login\"},\"channel\":\"/service/controller\"}";

            Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
            HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken, c.toString(), headers, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseString = response.body().string();
                        if (Kahoot.isDebug())
                            System.out.println("L1 = " + responseString);
                        JSONArray r2 = new JSONArray(responseString);
                        JSONObject r = r2.getJSONObject(r2.length() - 1);
                        boolean success = r.getBoolean("successful");
                        if (!success) {
                            System.out.println("[LOGIN/BEGIN] Error connecting to server! Full server response below.");
                            System.out.println(responseString);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    {
                        String headersData = "{\"clientId\":\"" + clientId + "\",\"channel\":\"/meta/connect\",\"connectionType\":\"long-polling\"}";
                        Headers headers = new Builder()
                                .add("clientId", clientId)
                                .add("channel", "/meta/connect")
                                .add("connectionType", "long-polling")
                                .add("Cookie", bayeuxCookie)
                                .build();

                        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken + "/connect", headersData, headers, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try {
                                    String responseString = response.body().string();
                                    if (Kahoot.isDebug())
                                        System.out.println("L2 = " + responseString);
                                    JSONArray r2 = new JSONArray(responseString);
                                    JSONObject r = r2.getJSONObject(r2.length() - 1);
                                    boolean success = r.getBoolean("successful");
                                    if (!success) {
                                        System.out.println("[LOGIN/FINISH] Error connecting to server! Full server response below.");
                                        System.out.println(responseString);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    }
                }
            });

        }

        active = true;
    }

    /**
     * Disconnect from the Kahoot game.<br>
     * It is possible to rejoin the Kahoot game if this function is called before the game is over.
     */
    public void disconnect() {
        JSONObject c = new JSONObject();
        c.put("channel", "/meta/disconnect");
        c.put("clientId", clientId);

        Headers headers = new Headers.Builder().add("Cookie", bayeuxCookie).build();
        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken + "/disconnect", c.toString(), headers, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    if (Kahoot.isDebug())
                        System.out.println("D = " + responseString);
                    JSONArray responseArray = new JSONArray(responseString);
                    JSONObject responseObject = responseArray.getJSONObject(0);
                    boolean success = responseObject.has("successful") && responseObject.getBoolean("successful");
                    if (!success) {
                        System.out.println("[DISCONNECT] Error connecting to server! Full server response below.");
                        System.out.println(responseString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        active = false;
    }

    private void answerQuestion(int ans) {
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
        content.put("choice", ans);
        content.put("meta", meta);

        //String content = "{\"choice\":" + ans + ",\"meta\":{\"lag\":22,\"device\":{\"userAgent\":\"" + HTTP.uagent + "\",\"screen\":{\"width\":360,\"height\":640}}}}";

        JSONObject data = new JSONObject();
        data.put("id", 6);
        data.put("type", "message");
        data.put("gameid", gamePin);
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
        HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken, base.toString(), headers, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseString = response.body().string();
                    if (Kahoot.isDebug())
                        System.out.println("AQ = " + responseString);
                    JSONArray r2 = new JSONArray(responseString);
                    JSONObject r = r2.getJSONObject(r2.length() - 1);
                    boolean success = r.getBoolean("successful");
                    if (!success) {
                        System.out.println("[QUESTION/ANSWER] Error connecting to server! Full server response below.");
                        System.out.println(responseString);
                    }
                    questionAnswered = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * Automatically answer questions with a randomly chosen answer.<br>
     * This function does the same thing as unixpickle's kahoot-rand program.<br>
     * <br>
     * <a href="https://github.com/unixpickle/kahoot-hack">unixpickle's Kahoot hack</a>
     */
    public void rand() {
        gamemode = 2;
        while (active) {
            JSONObject c2 = new JSONObject();
            c2.put("channel", "/meta/connect");
            c2.put("connectionType", "long-polling");
            c2.put("clientId", clientId);

            Headers headers = new Headers.Builder()
                    .add("Cookie", bayeuxCookie)
                    .build();
            HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken + "/connect", c2.toString(), headers, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseString = response.body().string();
                        if (Kahoot.isDebug())
                            System.out.println("R = " + responseString);
                        JSONArray r2 = new JSONArray(responseString);
                        JSONObject r = r2.getJSONObject(r2.length() - 1);
                        boolean success = r.getBoolean("successful");
                        if (!success) {
                            System.out.println("[LOGIN/FINISH] Error connecting to server! Full server response below.");
                            System.out.println(responseString);
                        }

                        if (responseString.contains("answerMap") && !responseString.contains("timeLeft")) {
                            JSONObject answerMap = r2.getJSONObject(0);
                            JSONObject data = answerMap.getJSONObject("data");
                            JSONObject content = new JSONObject(data.getString("content").replace("\\", ""));
                            JSONObject answers = content.getJSONObject("answerMap");
                            if (answers.length() == 2) {
                                optionTwoValid = false;
                                optionThreeValid = false;
                            }
                            if (answers.length() == 3) {
                                optionTwoValid = true;
                                optionThreeValid = false;
                            }
                            if (answers.length() == 4) {
                                optionTwoValid = true;
                                optionThreeValid = true;
                            }
                            Random random = new Random();
                            int answer = random.nextInt(answers.length());
                            int ranswer = answers.getInt(Integer.toString(answer));
                            Kahoot.this.answerQuestion(ranswer);
                            previousAnswer = answer;
                        } else if (responseString.contains("quizId")) {
                            active = false;
                        }

                        Thread.sleep(new Random().nextInt(5000));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Play the game as if you were playing it in your browser.<br>
     * Answers are organized in the following fashion:<br>
     * <br>
     * 0 1<br>
     * 2 3<br>
     * <br>
     * 2 and 3 are only there if the question has them.
     */
    public void play() {
        gamemode = 1;
        while (active) {
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

            HTTPUtils.POST(URL_BASE + gamePin + "/" + sessionToken + "/connect", postHeaders.toString(), headers, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseString = response.body().string();
                        if (Kahoot.isDebug())
                            System.out.println("response = " + responseString);
                        JSONArray responseArray = new JSONArray(responseString);
                        JSONObject r = responseArray.getJSONObject(responseArray.length() - 1);
                        JSONObject a = responseArray.getJSONObject(0);
                        boolean success = r.getBoolean("successful");
                        if (!success) {
                            System.out.println("[LOGIN/FINISH] Error connecting to server! Full server response below.");
                            System.out.println(responseString);
                        }
                        if (responseString.contains("answerMap") && !responseString.contains("timeLeft")) {
                            JSONObject data = a.getJSONObject("data");
                            JSONObject content = new JSONObject(data.getString("content").replace("\\", ""));
                            JSONObject answers = content.getJSONObject("answerMap");
                            if (answers.length() == 2) {
                                optionTwoValid = false;
                                optionThreeValid = false;
                            } else if (answers.length() == 3) {
                                optionTwoValid = true;
                                optionThreeValid = false;
                            } else {
                                optionTwoValid = true;
                                optionThreeValid = true;
                            }
                            System.out.println("Answers: 0 through " + (answers.length() - 1));
                            System.out.print("Answer: ");
                            int ans = userInput.nextInt();
                            previousAnswer = ans;
                            int ra = answers.getInt(Integer.toString(ans));
                            Kahoot.this.answerQuestion(ra);
                        } else if (responseString.contains("answerMap")) {
                            System.out.println("Get ready, question is coming up!");
                        }

                        if (responseString.contains("primaryMessage")) {
                            JSONObject data = a.getJSONObject("data");
                            JSONObject content = new JSONObject(data.getString("content").replace("\\", ""));
                            String primaryMessage = content.getString("primaryMessage");
                            System.out.println(primaryMessage);
                        } else if (responseString.contains("isCorrect")) {
                            JSONObject d = a.getJSONObject("data");
                            JSONObject c = new JSONObject(d.getString("content").replace("\\", ""));
                            boolean correct = c.getBoolean("isCorrect");
                            score = c.getInt("points");
                            totalScore = c.getInt("totalScore");
                            rank = c.getInt("rank");
                            Object rawNemesis = c.get("nemesis");
                            if (rawNemesis == null) {
                                nemesis = "no one";
                            } else {
                                nemesis = (String) rawNemesis;
                            }
                            System.out.println(correct ? "Correct!" : "Incorrect.");
                            System.out.println("You got " + score + " points from that question");
                            System.out.println("You currently have " + totalScore + " points");
                            System.out.println("You are in rank " + rank + ", behind " + nemesis);
                        } else if (responseString.contains("quizId")) {
                            JSONObject data = a.getJSONObject("data");
                            JSONObject content = new JSONObject(data.getString("content").replace("\\", ""));
                            String quizId = content.getString("quizId");
                            int playerCount = content.getInt("playerCount");
                            System.out.println("This quiz's ID is " + quizId);
                            System.out.println("Players in game: " + playerCount);
                            active = false;
                        }

                        Thread.sleep(50);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Game over!");
                }
            });
        }
    }

}