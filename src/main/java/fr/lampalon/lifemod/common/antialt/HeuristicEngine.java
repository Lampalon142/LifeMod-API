package fr.lampalon.lifemod.common.antialt;

import fr.lampalon.lifemod.common.antialt.analyzer.PhoneticAnalyzer;
import fr.lampalon.lifemod.common.antialt.analyzer.ShannonEntropyAnalyzer;
import fr.lampalon.lifemod.common.service.IConfigurationService;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeuristicEngine {

    private final IConfigurationService config;

    public HeuristicEngine(IConfigurationService config) {
        this.config = config;
    }

    public AnalysisResult analyze(String playerName, String ipAddress) {
        int score = 0;
        Set<HeuristicRule> rules = new HashSet<>();

        // 1. Entropy Analysis
        double entropy = ShannonEntropyAnalyzer.calculate(playerName);
        if (entropy > config.getDouble("modules.antialt.entropy-threshold", 3.8)) {
            score += config.getInt("modules.antialt.weights.entropy", 40);
            rules.add(HeuristicRule.HIGH_ENTROPY);
        }

        // 2. Phonetic Analysis
        if (PhoneticAnalyzer.containsBadBigrams(playerName)) {
            score += config.getInt("modules.antialt.weights.bigram", 25);
            rules.add(HeuristicRule.BAD_BIGRAM);
        }
        if (PhoneticAnalyzer.containsBadTrigrams(playerName)) {
            score += config.getInt("modules.antialt.weights.trigram", 35);
            rules.add(HeuristicRule.BAD_TRIGRAM);
        }

        // 3. Case Analysis
        if (hasRandomCasing(playerName)) {
            score += config.getInt("modules.antialt.weights.casing", 15);
            rules.add(HeuristicRule.RANDOM_CASING);
        }

        // 4. Suffix/Segmentation Analysis
        if (hasGeneratedSuffix(playerName)) {
            score += config.getInt("modules.antialt.weights.suffix", 50);
            rules.add(HeuristicRule.GENERATED_SUFFIX);
        }

        // ... (IP & Pattern history would be checked here using the database)

        score = Math.min(100, score);

        String fingerprint = generateFingerprint(playerName);
        
        return new AnalysisResult(playerName, score, rules, fingerprint);
    }

    private boolean hasRandomCasing(String name) {
        if (name.length() < 3) return false;
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i)) && Character.isUpperCase(name.charAt(i - 1))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGeneratedSuffix(String name) {
        Pattern pattern = Pattern.compile("[_-][a-zA-Z0-9]{4,8}$");
        Matcher matcher = pattern.matcher(name);
        return matcher.find();
    }

    private String generateFingerprint(String name) {
        String structure = name.replaceAll("[A-Z]", "U").replaceAll("[a-z]", "l").replaceAll("[0-9]", "d");
        return structure.length() + ":" + structure;
    }
}
