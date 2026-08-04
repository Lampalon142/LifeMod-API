package fr.lampalon.lifemod.api.antivpn;

import java.util.concurrent.CompletableFuture;

public interface IVpnService {

    CompletableFuture<IPInfo> lookup(String ip);

    CompletableFuture<Boolean> shouldAllowConnection(String ip, String playerName);

    CompletableFuture<Boolean> isProxy(String ip);
}