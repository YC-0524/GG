package com.group17.game.Utilities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public class HashUtil {
    public static String sha256Hash(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                str.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String generateRandomHash() {
        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder();
        // just fill with a bunch of random data
        stringBuilder.append(random.nextLong());
        stringBuilder.append(random.nextLong());
        stringBuilder.append(random.nextLong());
        return sha256Hash(stringBuilder.toString());
    }
}