package me.incognitojam.kahootj.examples;

import me.incognitojam.kahootj.Kahoot;
import me.incognitojam.kahootj.SessionUtils;

import java.util.Scanner;

public class ClientApp {

    private static final int BOTS_PER_SECOND = 10; // Maximum bots allowed to connect per second

    public static void main(String[] args) {
        System.out.print("Enter Game PIN: ");
        final Scanner stdin = new Scanner(System.in);
        final int gamepin = stdin.nextInt();
        stdin.nextLine(); // There is a newline character submitted with the int
        System.out.print("Checking game PIN validity... ");

        if (SessionUtils.checkPINValidity(gamepin)) {
            System.out.println("valid game PIN!");
        } else {
            System.out.println("invalid game PIN! Exiting.");
            return;
        }

        System.out.print("Username: ");
        String base = stdin.nextLine();

        System.out.println("[1] Play normally");
        System.out.println("[2] Flood with bots that randomly answer (If you join with enough bots, you're bound to win!)");
        System.out.print("Choice: ");
        int gm = stdin.nextInt();
        if (gm == 1) {
            Kahoot k = new Kahoot(base, gamepin, stdin, gm, false);
            k.initialize();
            System.out.println((k.isTeamGame() ? "Gamemode: TEAMS" : "Gamemode: CLASSIC"));
            k.start();
        } else if (gm == 2) {
            System.out.print("Number of bots: ");
            int botCount = stdin.nextInt();
            System.out.println("Confirmation: Entering with " + botCount + " bots.");

            Kahoot[] bots = new Kahoot[botCount];

            for (int i = 0; i < bots.length; i++) {
                String name = base + (i + 1);
                bots[i] = new Kahoot(name, gamepin, stdin, gm, true); // Instantly activate Kahoot object when botting. Otherwise this leads to bugs.
                System.out.print("Initializing Kahoot bots: " + (i + 1) + " / " + bots.length + "\r");
                try {
                    Thread.sleep(10); // Limit initializations to 100 bots per second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("");

            System.out.println((bots[0].isTeamGame() ? "Gamemode: TEAMS" : "Gamemode: CLASSIC"));

            for (int i = 0; i < bots.length; i++) {
                bots[i].start();
                System.out.print("Connecting Kahoot bots: " + (i + 1) + " / " + bots.length + "\r");
                try {
                    Thread.sleep(1000 / BOTS_PER_SECOND); // Rate limit sign ins to max_bps bots per second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("");

            System.out.println("All bots are in game. While the bots are running, the main thread will print answer statistics.");

            int quid = 0; //Question number

            int a = 0;
            int b = 0;
            int c = 0;
            int d = 0;

            while (bots[bots.length - 1].gameRunning()) { // while the last bot is still in the game...
                for (Kahoot bot : bots) { // ...get all answers submitted by the bots and count them up...
                    try {
                        int la = bot.getLastAnswerBlocking();
                        if (la == 0) {
                            a++;
                        } else if (la == 1) {
                            b++;
                        } else if (la == 2) {
                            c++;
                        } else if (la == 3) {
                            d++;
                        } else {
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (!bots[bots.length - 1].gameRunning()) {
                    break;
                }
                quid++;
                System.out.println("---QUESTION " + quid + " STATISTICS---"); // ..then display the statistics...
                System.out.println("Answer 0: " + a);
                System.out.println("Answer 1: " + b);
                System.out.println("Answer 2: " + c + (bots[bots.length - 1].wasLastQuestionAnswer2Valid() ? "" : "(invalid answer)"));
                System.out.println("Answer 3: " + d + (bots[bots.length - 1].wasLastQuestionAnswer3Valid() ? "" : "(invalid answer)"));
                a = 0; // ...finally clear the variables for the next count
                b = 0;
                c = 0;
                d = 0;
            }

            System.out.println("The game appears to have ended. Exiting the program!");
            System.exit(0);
        } else {
            System.out.println("Invalid choice. Exiting.");
        }
    }

}