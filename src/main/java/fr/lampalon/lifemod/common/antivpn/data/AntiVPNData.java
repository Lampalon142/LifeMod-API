package fr.lampalon.lifemod.common.antivpn.data;

import java.util.UUID;

public class AntiVPNData {

    private UUID uuid;

    private long ping;
    private String country;

    public UUID getUuid() {
        return uuid;
    }

    public long getPing() {
        return ping;
    }

    public String getCountry() {
        return country;
    }
}
