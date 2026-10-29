package com.group17.game.Utilities;

import java.util.Random;

public class PasswordFlows {
    public static String[] ADJECTIVES = {
        "Happy",
        "Sad",
        "Angry",
        "Upset",
        "Jolly",
        "Laughing",
        "Hungry",
        "Passive",
        "Aggressive"
    };

    public static String[] NOUNS = {
        "Cow",
        "Chicken",
        "Dog",
        "Rooster",
        "Fish",
        "Cat",
        "Bird",
        "Dinosaur"
    };

    public static int MAX_NUMBER = 100;

    public static String generateTemporaryPassowrd() {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ADJECTIVES[random.nextInt(0, ADJECTIVES.length)]);
        stringBuilder.append(NOUNS[random.nextInt(0, NOUNS.length)]);
        stringBuilder.append(random.nextInt(0, MAX_NUMBER));
        return stringBuilder.toString();
    }
}
