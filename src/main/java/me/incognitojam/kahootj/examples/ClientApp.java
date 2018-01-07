package me.incognitojam.kahootj.examples;

import me.incognitojam.kahootj.KahootClient;
import me.incognitojam.kahootj.actionprovider.UserActionProvider;
import me.incognitojam.kahootj.utils.SessionUtils;

import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) {
        System.out.print("Enter Game PIN: ");
        final Scanner input = new Scanner(System.in);
        final int gamePin = input.nextInt();
        input.nextLine(); // There is a newline character submitted with the int
        System.out.print("Checking game PIN validity... ");

        if (SessionUtils.checkPINValidity(gamePin)) {
            System.out.println("valid game PIN!");
        } else {
            System.out.println("invalid game PIN! Exiting.");
            return;
        }

        System.out.print("Username: ");
        String username = input.nextLine();

        KahootClient kahoot = new KahootClient(username, new UserActionProvider(input));
        kahoot.setGame(gamePin);
        kahoot.run();
    }

}