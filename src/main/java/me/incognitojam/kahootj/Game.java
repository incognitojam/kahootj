package me.incognitojam.kahootj;

public class Game {
    public boolean active;
    public int gamepin; // The game pin

    public int previousAnswer; // The last answer submitted to the game
    public int lastScore; // Score from last question
    public int totalScore; // Total score
    public int currentRank; // This Kahoot object's rank
    public String nemesis; // The person directly ahead of us. If there is no one ahead of us (1st place) this will be set to "no one"

    public boolean questionAnswered; // Question answered? prevents duplicate returns on getLastAnswerBlocking
    public boolean optionThreeValid; // Was 3 a valid answer on the last question?
    public boolean optionFourValid; // Was 4 a valid answer on the last question?
    public boolean team; // Is this a team game or classic PvP?

    public Game(int gamePin) {
        this.gamepin = gamePin;
    }

    @Override
    public String toString() {
        return "Game{" +
                "active=" + active +
                ", gamepin=" + gamepin +
                ", totalScore=" + totalScore +
                ", currentRank=" + currentRank +
                '}';
    }
}
