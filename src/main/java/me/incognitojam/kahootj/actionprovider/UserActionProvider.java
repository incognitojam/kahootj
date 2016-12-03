package me.incognitojam.kahootj.actionprovider;

import me.incognitojam.kahootj.Game;

import java.util.Scanner;

public class UserActionProvider implements IActionProvider {

    private Scanner scanner;

    public UserActionProvider(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public int getChoice(Game game) {
        int choice = -1;
        while (!isValid(choice, game)) {
            if (choice != -1) System.out.println("Invalid choice!");
            System.out.print("Please provide an option between 1 and " + getMax(game) + ".");
            choice = scanner.nextInt();
        }
        return choice - 1;
    }

    private boolean isValid(int choice, Game game) {
        int max = getMax(game);
        return choice <= max && choice >= 0;
    }

    private int getMax(Game game) {
        return game.optionThreeValid ? (game.optionFourValid ? 4 : 3) : 2;
    }

}
