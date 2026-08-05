package fr.lampalon.lifemod.api.antialt;

import java.util.List;

public class AntiAltResult {

    private final String playerName;
    private final int dangerScore;
    private final List<String> triggeredRuleKeys;
    private final List<String> triggeredRuleReasons;
    private final String fingerprint;

    public AntiAltResult(String playerName, int dangerScore, List<String> triggeredRuleKeys,
                         List<String> triggeredRuleReasons, String fingerprint) {
        this.playerName = playerName;
        this.dangerScore = dangerScore;
        this.triggeredRuleKeys = List.copyOf(triggeredRuleKeys);
        this.triggeredRuleReasons = List.copyOf(triggeredRuleReasons);
        this.fingerprint = fingerprint;
    }

    public String getPlayerName() { return playerName; }
    public int getDangerScore() { return dangerScore; }
    public List<String> getTriggeredRuleKeys() { return triggeredRuleKeys; }
    public List<String> getTriggeredRuleReasons() { return triggeredRuleReasons; }
    public String getFingerprint() { return fingerprint; }
}