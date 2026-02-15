package fr.lampalon.lifemod.common.antialt.analyzer;

import java.util.HashMap;
import java.util.Map;

public class ShannonEntropyAnalyzer {

    public static double calculate(String s) {
        if (s == null || s.isEmpty()) {
            return 0.0;
        }

        Map<Character, Integer> charCounts = new HashMap<>();
        for (char c : s.toCharArray()) {
            charCounts.put(c, charCounts.getOrDefault(c, 0) + 1);
        }

        double entropy = 0.0;
        for (char c : charCounts.keySet()) {
            double probability = (double) charCounts.get(c) / s.length();
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }

        return entropy;
    }
}
