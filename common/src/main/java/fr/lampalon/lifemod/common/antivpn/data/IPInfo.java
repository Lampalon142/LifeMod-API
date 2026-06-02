package fr.lampalon.lifemod.common.antivpn.data;

public class IPInfo {
    private String ip;
    private String countryCode;
    private String countryName;
    private String isp;
    private boolean proxy;
    private long lastUpdate;

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
