package fr.lampalon.lifemod.common.antialt;

import fr.lampalon.lifemod.common.antialt.analyzer.PhoneticAnalyzer;
import fr.lampalon.lifemod.common.antialt.analyzer.ShannonEntropyAnalyzer;
import fr.lampalon.lifemod.common.antivpn.AntiVPNService;
import fr.lampalon.lifemod.common.antivpn.data.IPInfo;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class HeuristicEngine {

    private final IConfigurationService config;
    private final Pattern generatedSuffixPattern;
    private final Pattern numericSuffixPattern;
    private final Pattern charRepetitionPattern;

    public HeuristicEngine(IConfigurationService config) {
        this.config = config;
        this.generatedSuffixPattern = Pattern.compile(config.getString("modules.antialt.regex-patterns.generated-suffix", "[_-][a-zA-Z0-9]{4,8}$"));
        this.numericSuffixPattern = Pattern.compile(config.getString("modules.antialt.regex-patterns.numeric-suffix", "[a-zA-Z]{3,}[0-9]{2,4}$"));
        this.charRepetitionPattern = Pattern.compile(config.getString("modules.antialt.regex-patterns.char-repetition", "(.)\\1{3,}"));
    }

    public CompletableFuture<AnalysisResult> analyze(UUID playerUuid, String playerName, String ipAddress) {
        DatabaseProvider db = ServiceRegistry.get(DatabaseProvider.class);
        ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
        AntiVPNService vpnService = ServiceRegistry.get(AntiVPNService.class);

        if (db == null || sanctionService == null) return CompletableFuture.completedFuture(null);

        List<PlayerData> alts = db.getAlts(ipAddress);
        long now = System.currentTimeMillis();

        List<CompletableFuture<Sanction>> banFutures = new ArrayList<>();
        List<PlayerData> relevantAlts = new ArrayList<>();
        if (alts != null) {
            for (PlayerData alt : alts) {
                if (alt.getUuid().equals(playerUuid)) continue;
                relevantAlts.add(alt);
                banFutures.add(sanctionService.getActiveSanction(alt.getUuid(), alt.getLastName(), SanctionType.BAN));
            }
        }

        CompletableFuture<IPInfo> vpnFuture = vpnService != null
                ? vpnService.getLookupManager().lookup(ipAddress)
                : CompletableFuture.completedFuture(null);

        List<CompletableFuture<?>> allFutures = new ArrayList<>(banFutures);
        allFutures.add(vpnFuture);

        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int score = 0;
                    Set<HeuristicRule> rules = new HashSet<>();
                    boolean vpnDetected = false;

                    // 1 & 2. Score IP exacte & Bonus temporel
                    for (int i = 0; i < relevantAlts.size(); i++) {
                        PlayerData alt = relevantAlts.get(i);
                        Sanction activeBan = banFutures.get(i).join();
                        if (activeBan != null) {
                            score += config.getInt("modules.antialt.weights.ip-exact-banned", 60);
                            rules.add(HeuristicRule.IP_EXACT_BANNED);

                            long diffMs = now - activeBan.getCreatedAt();
                            long diffHours = diffMs / (1000 * 60 * 60);
                            long diffDays = diffHours / 24;

                            if (diffHours <= config.getInt("modules.antialt.temporal.recent-ban-hours", 24)) {
                                score += config.getInt("modules.antialt.weights.ip-temporal-24h", 30);
                                rules.add(HeuristicRule.IP_TEMPORAL_RECENT);
                            } else if (diffDays <= config.getInt("modules.antialt.temporal.medium-ban-days", 7)) {
                                score += config.getInt("modules.antialt.weights.ip-temporal-week", 15);
                                rules.add(HeuristicRule.IP_TEMPORAL_RECENT);
                            }
                            break;
                        }
                    }

                    // 4. Score pseudo
                    score += analyzeUsername(playerName, rules);

                    // 5. Score historique IP
                    DatabaseProvider.IPReputation reputation = db.getIPReputation(ipAddress);
                    if (reputation != null && reputation.bannedAccounts >= 1) {
                        score += config.getInt("modules.antialt.weights.ip-history-multiple", 20);
                        rules.add(HeuristicRule.IP_HISTORY_MULTIPLE);
                    }

                    // 6. Score switch VPN soudain & 7. Multiplicateur VPN
                    try {
                        IPInfo ipInfo = vpnFuture.get();
                        if (ipInfo != null && ipInfo.isProxy()) {
                            vpnDetected = true;
                            rules.add(HeuristicRule.VPN_DETECTED);

                            PlayerData currentData = db.getPlayerData(playerUuid);
                            if (currentData != null) {
                                long daysSinceFirstSeen = (now - currentData.getFirstSeen()) / (1000 * 60 * 60 * 24);
                                if (daysSinceFirstSeen >= config.getInt("modules.antialt.sudden-vpn-days", 30)) {
                                    score += config.getInt("modules.antialt.weights.sudden-vpn-switch", 20);
                                    rules.add(HeuristicRule.SUDDEN_VPN_SWITCH);
                                }
                            }

                            if (score > config.getInt("modules.antialt.weights.vpn-score-threshold", 30)) {
                                score = (int) (score * config.getDouble("modules.antialt.weights.vpn-multiplier", 1.4));
                            } else {
                                score += config.getInt("modules.antialt.weights.vpn-alone", 15);
                            }
                        }
                    } catch (Exception e) {
                        java.util.logging.Logger.getLogger(getClass().getName()).warning("Heuristic scoring error for " + playerName + ": " + e.getMessage());
                    }

                    // 8. Réduction NAT automatique
                    if (config.getBoolean("modules.antialt.nat-detection.enabled", true)) {
                        int legitimateAccounts = db.getLegitimateAccountCount(ipAddress);
                        if (legitimateAccounts > config.getInt("modules.antialt.nat-detection.legitimate-threshold", 3)) {
                            double reduction = 1.0 / Math.log(legitimateAccounts + 1);
                            score = (int) (score * reduction);
                        }
                    }

                    score = Math.min(100, score);
                    String fingerprint = generateFingerprint(playerName, ipAddress, vpnDetected);

                    return new AnalysisResult(playerName, score, rules, fingerprint);
                });
    }

    private int analyzeUsername(String name, Set<HeuristicRule> rules) {
        int score = 0;
        
        // Entropie
        double entropy = ShannonEntropyAnalyzer.calculate(name);
        if (entropy > config.getDouble("modules.antialt.entropy-threshold", 3.8)) {
            score += config.getInt("modules.antialt.weights.pseudo-entropy", 20);
            rules.add(HeuristicRule.HIGH_ENTROPY);
        }

        // Phonétique
        if (PhoneticAnalyzer.containsBadBigrams(name)) {
            score += config.getInt("modules.antialt.weights.pseudo-bigram", 15);
            rules.add(HeuristicRule.BAD_BIGRAM);
        }
        if (PhoneticAnalyzer.containsBadTrigrams(name)) {
            score += config.getInt("modules.antialt.weights.pseudo-bigram", 15);
            rules.add(HeuristicRule.BAD_TRIGRAM);
        }
        if (PhoneticAnalyzer.hasImpossibleClusters(name)) {
            score += config.getInt("modules.antialt.weights.pseudo-bigram", 15);
            rules.add(HeuristicRule.BAD_BIGRAM);
        }

        // Regex
        if (generatedSuffixPattern.matcher(name).find()) {
            score += config.getInt("modules.antialt.weights.pseudo-suffix", 15);
            rules.add(HeuristicRule.GENERATED_SUFFIX);
        }
        if (numericSuffixPattern.matcher(name).find()) {
            score += config.getInt("modules.antialt.weights.pseudo-suffix", 15);
            rules.add(HeuristicRule.NUMERIC_SUFFIX);
        }
        if (charRepetitionPattern.matcher(name).find()) {
            score += config.getInt("modules.antialt.weights.pseudo-suffix", 15);
            rules.add(HeuristicRule.CHAR_REPETITION);
        }

        // Casse
        if (hasRandomCasing(name)) {
            score += config.getInt("modules.antialt.weights.pseudo-casing", 10);
            rules.add(HeuristicRule.RANDOM_CASING);
        }

        // Mots troll
        List<String> trollPatterns = config.getStringList("modules.antialt.troll-patterns");
        String lowerName = name.toLowerCase();
        for (String pattern : trollPatterns) {
            if (lowerName.contains(pattern.toLowerCase())) {
                score += config.getInt("modules.antialt.weights.pseudo-troll", 25);
                rules.add(HeuristicRule.TROLL_NAME);
                break;
            }
        }

        return score;
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

    private String generateFingerprint(String name, String ip, boolean vpn) {
        return name.length() + ":" + (vpn ? "V" : "R") + ":" + name.replaceAll("[A-Z]", "U").replaceAll("[a-z]", "l").replaceAll("[0-9]", "d");
    }
}
