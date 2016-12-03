package me.incognitojam.kahootj.examples;

import me.incognitojam.kahootj.KahootClient;
import me.incognitojam.kahootj.SessionUtils;
import me.incognitojam.kahootj.actionprovider.IActionProvider;
import me.incognitojam.kahootj.actionprovider.RandomActionProvider;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloodApp {

    private static final int BOTS_PER_SECOND = 25;

    public static void main(String[] args) throws IOException {
        System.out.print("Enter Game PIN: ");
        final Scanner userInput = new Scanner(System.in);
        final int gamepin = userInput.nextInt();
        userInput.nextLine(); // There is a newline character submitted with the int
        System.out.print("Checking game PIN validity... ");
        if (SessionUtils.checkPINValidity(gamepin)) {
            System.out.println("valid game PIN!");
        } else {
            System.out.println("invalid game PIN! Exiting.");
            return;
        }

        System.out.print("Username: ");
        String base = userInput.nextLine();

        System.out.print("Number of bots: ");
        int botCount = userInput.nextInt();
        System.out.println("Confirmation: Entering with " + botCount + " bots.");

        ExecutorService executor = Executors.newCachedThreadPool();
        IActionProvider actionProvider = new RandomActionProvider();
        KahootClient[] bots = new KahootClient[botCount];

        for (int i = 0; i < bots.length; i++) {
            String name = base + "16" + (1000 + new Random().nextInt(8999));
            bots[i] = new KahootClient(name, actionProvider); // Instantly activate Kahoot object when botting. Otherwise this leads to bugs.

            System.out.print("Initializing Kahoot bots: " + (i + 1) + " / " + bots.length + "\r");
            try {
                Thread.sleep(5); // Limit initializations to 200 bots per second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < bots.length; i++) {
            KahootClient bot = bots[i];
            bot.setGame(gamepin);
            executor.submit(bot);
            System.out.print("Connecting Kahoot bots: " + (i + 1) + " / " + bots.length + "\r");
            try {
                Thread.sleep(1000 / BOTS_PER_SECOND); // Rate limit sign ins to max_bps bots per second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while (!botsReady(bots)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n" + (bots[0].isTeamGame() ? "Gamemode: TEAMS" : "Gamemode: CLASSIC"));
        System.out.println("\nAll bots are in game. While the bots are running, the main thread will print answer statistics.");

        // Question number
        int questionId = 0;

        int a = 0;
        int b = 0;
        int c = 0;
        int d = 0;

        while (bots[bots.length - 1].isGameRunning()) { // while the last bot is still in the game...
            for (KahootClient bot : bots) { // ...get all answers submitted by the bots and count them up...
                try {
                    int lastAnswer = bot.getLastAnswerBlocking();
                    if (lastAnswer == 0) {
                        a++;
                    } else if (lastAnswer == 1) {
                        b++;
                    } else if (lastAnswer == 2) {
                        c++;
                    } else if (lastAnswer == 3) {
                        d++;
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!bots[bots.length - 1].isGameRunning()) {
                break;
            }
            questionId++;
            System.out.println("---QUESTION " + questionId + " STATISTICS---"); //..then display the statistics...
            System.out.println("Answer 1: " + a);
            System.out.println("Answer 2: " + b);
            System.out.println("Answer 3: " + c + (bots[bots.length - 1].getGame().optionThreeValid ? "" : "(invalid answer)"));
            System.out.println("Answer 4: " + d + (bots[bots.length - 1].getGame().optionFourValid ? "" : "(invalid answer)"));
            a = 0; //...finally clear the variables for the next count
            b = 0;
            c = 0;
            d = 0;
        }

        System.out.println("The game appears to have ended. Exiting the program!");
        System.exit(0);
    }

    private static boolean botsReady(KahootClient[] bots) {
        for (KahootClient client : bots)
            if (!client.isGameRunning()) return false;
        return true;
    }

}
