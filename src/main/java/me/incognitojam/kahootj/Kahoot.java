package me.incognitojam.kahootj;

@Deprecated
public class Kahoot extends Thread {

//    private final String URL_BASE = "https://kahoot.it/cometd/";
//    private String username; // This Kahoot object's username
//    private String clientId; // Unique client ID assigned to Kahoot clients
//    private String sessionToken; // This Kahoot object's session token
//    private String bayeuxCookie;
//    private boolean active = false; // Whether this Kahoot object is engaged in an active game or not
//    private int gamePin; // The game pin
//    private Scanner userInput; // A scanner hopefully scanning System.in
//    private int gamemode; // This Kahoot object's operation mode. 1 = play normally, 2 = auto answer questions randomly
//    private int previousAnswer; // The last answer submitted to the game
//    private int score; // Score from last question
//    private int totalScore; // Total score
//    private int rank; // This Kahoot object's rank
//    private String nemesis; // The person directly ahead of us. If there is no one ahead of us (1st place) this will be set to "no one"
//    private boolean questionAnswered; // Question answered? prevents duplicate returns on getLastAnswerBlocking
//    private boolean optionThreeValid = false; // Was 2 a valid answer on the last question?
//    private boolean optionFourValid = false; // Was 3 a valid answer on the last question?
//    private boolean isTeam = false; // Is this a team game or classic PvP?
//
//    private static boolean debuggingMode = false; //Connection debug mode, not useful to regular users

//    /**
//     * Construct a new Kahoot object. The newly constructed object can be thought of as a computer player.<br>
//     * This Kahoot object can act as a bot that automatically answers questions randomly.<br>
//     * It can also act as a regular player, depending on input from the user.
//     *
//     * @param username  the username that this Kahoot object will use to connect to the game
//     * @param gamePin   the Kahoot.it game PIN
//     * @param userInput a Scanner scanning System.in, if this parameter is not scanning System.in, expect bugs and even crashes.
//     * @param gamemode  the gamemode. 1 = play normally, 2 = auto answer questions randomly, anything else is invalid
//     * @param active    whether the object should instantly be active. If unsure, set to false.
//     */
//    public Kahoot(String username, int gamePin, Scanner userInput, int gamemode, boolean active) {
//        this.username = username;
//        this.userInput = userInput;
//        this.gamemode = gamemode;
//        this.active = active;
//        this.gamePin = gamePin;
//    }

//    /**
//     * Start this Kahoot object in a new thread, making concurrency easier.
//     */
//    public void run() {
//        if (gamemode == 1) {
//            // Auto initialization is unnecessary as this Kahoot object is registered as a regular user.
//            this.login();
//            System.out.println("You should be in game, see your name on screen?");
//            this.play();
//        } else if (gamemode == 2) {
//            this.initialize();
//            this.login();
//            this.rand();
//        }
//        // Any other gamemode is invalid, so disconnect right after login.
//        this.disconnect();
//    }

//    public static boolean isDebug() {
//        return debuggingMode;
//    }

//    public static void setDebug(boolean debuggingMode) {
//        Kahoot.debuggingMode = debuggingMode;
//    }

//    /**
//     * Check if answer 2 was a valid answer on the last question.
//     *
//     * @return true if answer 2 was valid, otherwise false
//     */
//    public boolean wasLastQuestionAnswer2Valid() {
//        return optionThreeValid;
//    }

//    /**
//     * Check if answer 3 was a valid answer on the last question.
//     *
//     * @return true if answer 3 was valid, otherwise false
//     */
//    public boolean wasLastQuestionAnswer3Valid() {
//        return optionFourValid;
//    }

//    /**
//     * Check if the game is running (in progress)
//     *
//     * @return true if the game is running, otherwise false
//     */
//    public boolean gameRunning() {
//        return active;
//    }

//    /**
//     * Get the last answer submitted to a question by this Kahoot object.<br>
//     * <br>
//     * WARNING: This function is blocking, meaning it will halt execution (unless this object is on its own thread) until a question is answered. This function is useful for counting answers submitted by the rand() function.<br>
//     * <br>
//     * This function can carry a delay of 10 milliseconds before it returns, but Kahoot questions take more than 1000 milliseconds to load, so this shouldn't be a problem.
//     *
//     * @return last answer submitted
//     * @throws InterruptedException if sleep is interrupted
//     */
//    public int getLastAnswerBlocking() throws InterruptedException {
//        while (!questionAnswered) {
//            // wait until qa is true
//            Thread.sleep(10);
//            if (!this.active)
//                return -1; // halt if the game isn't active.
//        }
//        questionAnswered = false;
//        return previousAnswer;
//    }

//    /**
//     * Get the last answer submitted to a question by this Kahoot object.
//     * <p>
//     * WARNING: This function is non-blocking, meaning it can return the same answer for the same question twice. If you want a blocking function, check getLastAnswerBlocking()
//     *
//     * @return last answer submitted
//     */
//    public int getLastAnswer() {
//        questionAnswered = false;
//        return previousAnswer;
//    }

//    /**
//     * Get the score from the last question answered
//     *
//     * @return score gained from last answered question
//     */
//    public int getLastScore() {
//        return score;
//    }

//    /**
//     * Get your total score between all questions
//     *
//     * @return total score
//     */
//    public int getTotalScore() {
//        return totalScore;
//    }

//    /**
//     * Get your rank
//     *
//     * @return your rank
//     */
//    public int getRank() {
//        return rank;
//    }

//    /**
//     * Get this Kahoot object's nemesis, or the player directly ahead of it.
//     *
//     * @return the player directly ahead of this Kahoot object
//     */
//    public String getNemesis() {
//        return nemesis;
//    }

//    /**
//     * Check whether this game is a team game or not
//     *
//     * @return true if team game, false if classic PvP
//     */
//    public boolean isTeamGame() {
//        return isTeam;
//    }

//    /**
//     * Initialize this Kahoot object and connect to the server.<br>
//     * This function does the same thing as if you were to enter the game PIN on the Kahoot website, but it does not log you in.<br>
//     * <br>
//     * This function must be called regardless of whether the bot will be allowed to manage itself or not. Check the run() documentation for more information.
//     */
//    public void initialize() {
//        sessionToken = SessionUtils.getSessionToken(gamePin);
//        sessionToken = SessionUtils.decodeSessionToken(sessionToken);
//
//        isTeam = SessionUtils.getLastGameTeam();
//
//        if (Kahoot.isDebug()) {
//            System.out.println("stoken = " + sessionToken);
//            System.out.println("gameid = " + gamePin);
//        }
//
//        initStageOne();
//    }

//    /**
//     * Login to the Kahoot game.<br>
//     * This function is does the same thing as if you were to enter your nickname in the second screen you would see in your browser.
//     */
//    public void login() {
//
//    }

//    /**
//     * Disconnect from the Kahoot game.<br>
//     * It is possible to rejoin the Kahoot game if this function is called before the game is over.
//     */
//    public void disconnect() {
//
//    }

}