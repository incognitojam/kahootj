package me.incognitojam.kahootj.examples;

import java.util.regex.Pattern;

public class ChallengeDecoder {

    public static void main(String[] args) {
        System.out.println(decodeChallenge("decode('reSOJgAFZmrkQlz2vn4QH8fMhYiz70dNkce5KjL3VbC4bOdNMQnZR6RvtDbnNtzvMh3VRSLFH4C4MndNVkxwFMYcu5vZrUDgFfmK'); function decode(message) {return _.replace(message, /./g, function(char, position) {return String.fromCharCode((((char.charCodeAt(0) * position) + 7) % 77) + 48)});}"));
    }

    private static String decodeChallenge(String challenge) {
        String[] parts = challenge.split("'");
        String numberString = challenge.split(Pattern.quote("position) + "))[1].split(Pattern.quote(") % "))[0];
        int number = Integer.valueOf(numberString);
//        System.out.println(number);

        String decodable = parts[1];
        System.out.println(decodable);

        String result = "";
        char[] array = decodable.toCharArray();
        for (int i = 0; i < array.length; i++) {
            char c = array[i];
            int val = (c * i) + number;
            System.out.println("Val A: " + val);
            val = val % 77;
            System.out.println("Val B: " + val);
            val += 48;
            System.out.println("Val C: " + val);
            result = result + String.valueOf((char) val);
        }

        return result;
    }

}
