package fr.lampalon.lifemod.api.scan;

import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface IScanService {

    CompletableFuture<ScanResult> scan(ScanType type, String target, ItemStack targetItem, Consumer<String> progressCallback);

    boolean isMatch(ItemStack item, ItemStack target);

    void cancel();

    boolean isCancelled();
}
