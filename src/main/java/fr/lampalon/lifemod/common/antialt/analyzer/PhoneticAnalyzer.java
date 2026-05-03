package fr.lampalon.lifemod.common.antialt.analyzer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class PhoneticAnalyzer {

    private static final Set<String> FORBIDDEN_BIGRAMS = new HashSet<>(Arrays.asList(
            "qg", "qx", "qj", "qz", "jc", "jg", "jk", "jq", "jv", "jx", "jz",
            "vx", "wx", "zx", "zj", "xj", "xb", "xc", "xd", "xf", "xg", "xh",
            "xk", "xm", "xn", "xp", "xq", "xr", "xs", "xt", "xv", "xw", "xz"
    ));
    
    private static final Set<String> FORBIDDEN_TRIGRAMS = new HashSet<>(Arrays.asList(
            "nqx", "lgv", "xjw", "kjp", "wzq", "vqc", "jxz", "yqx", "zxp", "kvj"
    ));

    private static final Pattern CONSONANT_CLUSTER = Pattern.compile("[bcdfghjklmnpqrstvwxyz]{5,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOWEL_CLUSTER = Pattern.compile("[aeiouy]{5,}", Pattern.CASE_INSENSITIVE);

    public static boolean containsBadBigrams(String s) {
        if (s == null || s.length() < 2) return false;
        String clean = s.toLowerCase().replaceAll("[^a-z]", "");
        for (int i = 0; i < clean.length() - 1; i++) {
            if (FORBIDDEN_BIGRAMS.contains(clean.substring(i, i + 2))) return true;
        }
        return false;
    }
    
    public static boolean containsBadTrigrams(String s) {
        if (s == null || s.length() < 3) return false;
        String clean = s.toLowerCase().replaceAll("[^a-z]", "");
        for (int i = 0; i < clean.length() - 2; i++) {
            if (FORBIDDEN_TRIGRAMS.contains(clean.substring(i, i + 3))) return true;
        }
        return false;
    }

    public static boolean hasImpossibleClusters(String s) {
        if (s == null) return false;
        return CONSONANT_CLUSTER.matcher(s).find() || VOWEL_CLUSTER.matcher(s).find();
    }
    
    public static boolean isGibberish(String s) {
        if (s == null || s.length() < 4) return false;
        return containsBadBigrams(s) || containsBadTrigrams(s) || hasImpossibleClusters(s);
    }
}
