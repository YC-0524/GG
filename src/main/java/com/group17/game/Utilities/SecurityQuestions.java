package com.group17.game.Utilities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

public class SecurityQuestions {
    public static final int REQUIRED_COUNT = 3;
    public static final int MIN_ANSWER_LENGTH = 3;

    public static final List<String> QUESTIONS = List.of(
        "What was the name of your first pet?",
        "What street did you live on in third grade?",
        "What was the name of your first-grade teacher?",
        "What was your childhood nickname?",
        "What is the name of the first school you attended?",
        "What was the make and model of your family's first car?",
        "What was the first concert you attended?",
        "What was the first video game you ever played?",
        "What was the name of your first stuffed animal or toy?",
        "In what city did you have your first job?",
        "What was your favourite food as a child?",
        "What was the name of your favourite childhood book?",
        "What was your dream job as a child?",
        "Who was your childhood hero?"
    );

    private SecurityQuestions() {}

    public static String normalize(String answer) {
        if (answer == null) return "";

        return answer.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static String hashAnswer(long userID, String answer) {
        return HashUtil.sha256Hash(userID + ":" + normalize(answer));
    }

    public static boolean matches(long userID, String answer, String storedHash) {
        return hashAnswer(userID, answer).equals(storedHash);
    }

    public static List<String> formValues(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? List.of() : Arrays.asList(values);
    }

    // returns error message if answers aren't valid
    public static Optional<String> validate(List<String> questions, List<String> answers) {
        if (questions == null || answers == null || questions.size() != REQUIRED_COUNT || answers.size() != REQUIRED_COUNT) {
            return Optional.of("Please choose " + REQUIRED_COUNT + " security questions and answer each one.");
        }

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < REQUIRED_COUNT; i++) {
            String question = questions.get(i);

            if (!QUESTIONS.contains(question)) {
                return Optional.of("Please pick each security question from the list.");
            }

            if (!seen.add(question)) {
                return Optional.of("Please choose " + REQUIRED_COUNT + " different security questions");
            }

            if (normalize(answers.get(i)).length() < MIN_ANSWER_LENGTH) {
                return Optional.of("Each security answer needs at least " + MIN_ANSWER_LENGTH + " characters.");
            }
        }

        return Optional.empty();
    }
}
