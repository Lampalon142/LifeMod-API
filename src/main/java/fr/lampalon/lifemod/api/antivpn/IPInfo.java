package fr.lampalon.lifemod.api.antivpn;

public class IPInfo {

    private final String ip;
    private final String countryCode;
    private final String countryName;
    private final String isp;
    private final boolean proxy;
    private final long lastUpdate;

    public IPInfo(String ip, String countryCode, String countryName, String isp, boolean proxy, long lastUpdate) {
        this.ip = ip;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.isp = isp;
        this.proxy = proxy;
        this.lastUpdate = lastUpdate;
    }

    public String getIp() { return ip; }
    public String getCountryCode() { return countryCode; }
    public String getCountryName() { return countryName; }
    public String getIsp() { return isp; }
    public boolean isProxy() { return proxy; }
    public long getLastUpdate() { return lastUpdate; }
}