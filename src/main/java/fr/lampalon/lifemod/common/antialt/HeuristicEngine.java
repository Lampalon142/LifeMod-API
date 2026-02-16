package fr.lampalon.lifemod.common.antialt;

import fr.lampalon.lifemod.common.antialt.analyzer.PhoneticAnalyzer;
import fr.lampalon.lifemod.common.antialt.analyzer.ShannonEntropyAnalyzer;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ISanctionService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeuristicEngine {

    private final IConfigurationService config;

    public HeuristicEngine(IConfigurationService config) {
        this.config = config;
    }

    public CompletableFuture<AnalysisResult> analyze(String playerName, String ipAddress) {
        return CompletableFuture.supplyAsync(() -> {
            int score = 0;
            Set<HeuristicRule> rules = new HashSet<>();

            // 1. Entropie de Shannon
            double entropy = ShannonEntropyAnalyzer.calculate(playerName);
            if (entropy > config.getDouble("modules.antialt.entropy-threshold", 3.8)) {
                score += config.getInt("modules.antialt.weights.entropy", 40);
                rules.add(HeuristicRule.HIGH_ENTROPY);
            }

            // 2. Analyse Phonétique
            if (PhoneticAnalyzer.containsBadBigrams(playerName)) {
                score += config.getInt("modules.antialt.weights.bigram", 25);
                rules.add(HeuristicRule.BAD_BIGRAM);
            }
            if (PhoneticAnalyzer.containsBadTrigrams(playerName)) {
                score += config.getInt("modules.antialt.weights.trigram", 35);
                rules.add(HeuristicRule.BAD_TRIGRAM);
            }

            if (hasRandomCasing(playerName)) {
                score += config.getInt("modules.antialt.weights.casing", 15);
                rules.add(HeuristicRule.RANDOM_CASING);
            }

            if (hasGeneratedSuffix(playerName)) {
                score += config.getInt("modules.antialt.weights.suffix", 50);
                rules.add(HeuristicRule.GENERATED_SUFFIX);
            }

            try {
                DatabaseProvider db = ServiceRegistry.get(DatabaseProvider.class);
                ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
                
                if (db != null && sanctionService != null) {
                    List<PlayerData> alts = db.getAlts(ipAddress);
                    if (alts != null) {
                        long bannedAlts = alts.stream().filter(alt -> {
                            var sanction = sanctionService.getActiveSanction(alt.getUuid(), alt.getLastName(), SanctionType.BAN);
                            return sanction != null && sanction.join() != null;
                        }).count();

                        if (bannedAlts > 0) {
                            score += config.getInt("modules.antialt.weights.ip-history", 50);
                            rules.add(HeuristicRule.IP_HISTORY);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore DB errors but log if critical
            }

            score = Math.min(100, score);
            String fingerprint = generateFingerprint(playerName);
            return new AnalysisResult(playerName, score, rules, fingerprint);
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    private boolean hasRandomCasing(String name) {
        if (name.length() < 3) return false;
        int upperCount = 0;
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                if (!Character.isLetter(name.charAt(i-1)) || Character.isUpperCase(name.charAt(i-1))) {
                    return true; 
                }
            }
        }
        return false;
    }

    private boolean hasGeneratedSuffix(String name) {
        return Pattern.compile("[_-][a-zA-Z0-9]{4,8}$").matcher(name).find();
    }

    private String generateFingerprint(String name) {
        return name.length() + ":" + name.replaceAll("[A-Z]", "U").replaceAll("[a-z]", "l").replaceAll("[0-9]", "d");
    }
}
