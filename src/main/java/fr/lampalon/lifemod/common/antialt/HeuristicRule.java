package fr.lampalon.lifemod.common.antialt;

public enum HeuristicRule {
    HIGH_ENTROPY("EntropyHigh", "High name entropy"),
    BAD_BIGRAM("BadBigram", "Uncommon bigrams"),
    BAD_TRIGRAM("BadTrigram", "Uncommon trigrams"),
    GENERATED_SUFFIX("GeneratedSuffix", "Suffix matches known generator patterns"),
    NUMERIC_SUFFIX("NumericSuffix", "Numeric suffix pattern detected"),
    CHAR_REPETITION("CharRepetition", "Excessive character repetition"),
    RANDOM_CASING("RandomCasing", "Random capitalization detected"),
    TROLL_NAME("TrollName", "Username contains troll/blacklisted words"),
    IP_EXACT_BANNED("IPExactBanned", "IP is linked to an active ban"),
    IP_TEMPORAL_RECENT("IPTemporalRecent", "Connection shortly after a ban from this IP"),
    IP_SUBNET_BANNED("IPSubnetBanned", "Subnet /24 has a history of bans"),
    IP_HISTORY_MULTIPLE("IPHistoryMultiple", "IP has generated multiple banned accounts"),
    VPN_DETECTED("VPNDetected", "Connection via VPN/Proxy"),
    SUDDEN_VPN_SWITCH("SuddenVPNSwitch", "Known player suddenly using a VPN");

    private final String key;
    private final String reason;

    HeuristicRule(String key, String reason) {
        this.key = key;
        this.reason = reason;
    }

    public String getKey() {
        return key;
    }

    public String getReason() {
        return reason;
    }
}
