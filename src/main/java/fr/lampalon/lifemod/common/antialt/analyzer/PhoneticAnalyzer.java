package fr.lampalon.lifemod.common.antialt.analyzer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PhoneticAnalyzer {

    // Common English bigram frequencies are high for "th", "he", "in", "er", "an", "re", etc.
    // Generated names often use improbable combinations.
    private static final Set<String> FORBIDDEN_BIGRAMS = new HashSet<>(Arrays.asList(
            "qg", "qx", "qj", "qz", "jc", "jg", "jk", "jq", "jv", "jx", "jz",
            "vx", "wx", "zx", "zj", "x
j"
    ));
    
    private static final Set<String> FORBIDDEN_TRIGRAMS = new HashSet<>(Arrays.asList(
            "nqx", "lgv", "xjw"
    ));

    public static boolean containsBadBigrams(String s) {
        if (s == null || s.length() < 2) {
            return false;
        }
        s = s.toLowerCase();
        for (int i = 0; i < s.length() - 1; i++) {
            if (FORBIDDEN_BIGRAMS.contains(s.substring(i, i + 2))) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean containsBadTrigrams(String s) {
        if (s == null || s.length() < 3) {
            return false;
        }
        s = s.toLowerCase();
        for (int i = 0; i < s.length() - 2; i++) {
            if (FORBIDDEN_TRIGRAMS.contains(s.substring(i, i + 3))) {
                return true;
            }
        }
        return false;
    }
}
