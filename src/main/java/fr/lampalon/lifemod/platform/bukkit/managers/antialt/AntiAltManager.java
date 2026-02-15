package fr.lampalon.lifemod.platform.bukkit.managers.antialt;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.antialt.HeuristicRule;
import fr.lampalon.lifemod.common.antialt.analyzer.PhoneticAnalyzer;
import fr.lampalon.lifemod.common.antialt.analyzer.ShannonEntropyAnalyzer;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntiAltManager {

    private final LifeMod plugin;

    public AntiAltManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    public AnalysisResult analyze(Player player) {
        String name = player.getName();
        int score = 0;
        Set<HeuristicRule> rules = new HashSet<>();

        // 1. Entropy Analysis
        double entropy = ShannonEntropyAnalyzer.calculate(name);
        if (entropy > plugin.getConfigConfig().getDouble("modules.antialt.entropy-threshold", 3.8)) {
            score += 40;
            rules.add(HeuristicRule.HIGH_ENTROPY);
        }

        // 2. Phonetic Analysis
        if (PhoneticAnalyzer.containsBadBigrams(name)) {
            score += 25;
            rules.add(HeuristicRule.BAD_BIGRAM);
        }
        if (PhoneticAnalyzer.containsBadTrigrams(name)) {
            score += 35;
            rules.add(HeuristicRule.BAD_TRIGRAM);
        }

        // 3. Case Analysis
        if (hasRandomCasing(name)) {
            score += 15;
            rules.add(HeuristicRule.RANDOM_CASING);
        }

        // 4. Suffix/Segmentation Analysis
        if (hasGeneratedSuffix(name)) {
            score += 50;
            rules.add(HeuristicRule.GENERATED_SUFFIX);
        }
        
        // ... (IP & Pattern history to be added later)

        score = Math.min(100, score); // Cap score at 100

        String fingerprint = generateFingerprint(name);
        
        AnalysisResult result = new AnalysisResult(name, score, rules, fingerprint);
        
        // Execute reaction based on score
        executeReaction(result);

        return result;
    }
    
    private void executeReaction(AnalysisResult result) {
        String playerName = result.getPlayerName();
        int score = result.getDangerScore();
        String reason = result.getReason();
        String fingerprint = result.getFingerprint();

        // This is where the logic from config will be executed.
        // For now, let's just log it if debug is on.
        if (plugin.getDebugManager().isModDebug("antialt")) {
            plugin.getLogger().info("[AntiAlt Debug] Player: " + playerName + " | Score: " + score + " | Reason: " + reason + " | Fingerprint: " + fingerprint);
        }

        // Example reaction logic (to be moved to a script handler)
        if (score > 85) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + playerName + " High-Threat Alt Account (Score: " + score + ", Reason: " + reason + ")");
        } else if (score > 60) {
            String message = "&c[AntiAlt] &e" + playerName + " &cest fortement suspect (Score: " + score + ").";
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("lifemod.antialt.notify"))
                .forEach(p -> p.sendMessage(message));
        }
    }

    private boolean hasRandomCasing(String name) {
        if (name.length() < 3) return false;
        // Check for non-starting uppercase letters that don't follow a lowercase one (ignores CamelCase)
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i)) && Character.isUpperCase(name.charAt(i - 1))) {
                return true; // e.g., XX
            }
        }
        return false;
    }

    private boolean hasGeneratedSuffix(String name) {
        // Look for _ or - followed by 4-8 alphanumeric chars
        Pattern pattern = Pattern.compile("[_-][a-zA-Z0-9]{4,8}$");
        Matcher matcher = pattern.matcher(name);
        return matcher.find();
    }
    
    private String generateFingerprint(String name) {
        // Simple fingerprint based on character types and length
        String structure = name.replaceAll("[A-Z]", "U").replaceAll("[a-z]", "l").replaceAll("[0-9]", "d");
        return structure.length() + ":" + structure;
    }
}
