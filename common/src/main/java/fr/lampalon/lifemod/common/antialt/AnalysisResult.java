package fr.lampalon.lifemod.common.antialt;

import java.util.Set;

public class AnalysisResult {
    private final String playerName;
    private final int dangerScore;
    private final Set<HeuristicRule> triggeredRules;
    private final String fingerprint;

    public AnalysisResult(String playerName, int dangerScore, Set<HeuristicRule> triggeredRules, String fingerprint) {
        this.playerName = playerName;
        this.dangerScore = dangerScore;
        this.triggeredRules = triggeredRules;
        this.fingerprint = fingerprint;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getDangerScore() {
        return dangerScore;
    }

    public Set<HeuristicRule> getTriggeredRules() {
        return triggeredRules;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getReason() {
        if (triggeredRules.isEmpty()) {
            return "N/A";
        }
        return triggeredRules.iterator().next().getReason();
    }
}
