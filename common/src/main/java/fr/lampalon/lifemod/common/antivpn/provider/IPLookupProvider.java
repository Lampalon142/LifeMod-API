package fr.lampalon.lifemod.common.antivpn.provider;

import fr.lampalon.lifemod.common.antivpn.data.IPInfo;
import java.util.concurrent.CompletableFuture;

public interface IPLookupProvider {
    CompletableFuture<IPInfo> lookup(String ip);
    String getName();
}
