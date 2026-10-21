package com.example.myapplication;

import java.security.MessageDigest;
import java.security.SecureRandom;

public class PasswordUtils {

    // Generates a cryptographically secure random 16-byte salt as a Hex string
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return bytesToHex(saltBytes);
    }

    // Hashes password + salt using SHA-256 algorithm
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null || password.trim().isEmpty() || salt.trim().isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(hexToBytes(salt));
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Comparison Verification Algorithm: verifies if entered password matches stored hash
    public static boolean verifyPassword(String enteredPassword, String storedHash, String storedSalt) {
        if (enteredPassword == null || storedHash == null || storedSalt == null) {
            return false;
        }
        if (enteredPassword.trim().isEmpty() || storedHash.trim().isEmpty() || storedSalt.trim().isEmpty()) {
            return false; // Prevent empty/blank hash verification for Google accounts or blank passwords
        }
        String calculatedHash = hashPassword(enteredPassword, storedSalt);
        return calculatedHash != null && storedHash.equalsIgnoreCase(calculatedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
