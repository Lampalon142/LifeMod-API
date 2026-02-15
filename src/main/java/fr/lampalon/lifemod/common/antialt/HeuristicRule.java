package fr.lampalon.lifemod.common.antialt;

public enum HeuristicRule {
    HIGH_ENTROPY("EntropyHigh", "High name entropy"),
    BAD_BIGRAM("BadBigram", "Uncommon bigrams"),
    BAD_TRIGRAM("BadTrigram", "Uncommon trigrams"),
    GENERATED_SUFFIX("GeneratedSuffix", "Suffix matches known generator patterns"),
    RANDOM_CASING("RandomCasing", "Random capitalization detected"),
    IP_HISTORY("IPHistory", "IP has a history of suspicious accounts"),
    PATTERN_MATCH("PatternMatch", "Username matches a known threat pattern");

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
