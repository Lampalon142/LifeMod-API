package fr.lampalon.lifemod.common.anticheat.check;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import java.util.UUID;

/**
 * Base implementation of a Check with common logic for configuration and flagging.
 */
public abstract class AbstractCheck implements Check {

    protected final String name;
    protected final IConfigurationService configService;
    private final String configPath;

    public AbstractCheck(String name, IConfigurationService configService) {
        this.name = name;
        this.configService = configService;
        this.configPath = "anticheat.checks." + name.toLowerCase();
    }

    @Override
    public boolean isAsync() {
        return false; // Default: sync for reliability, override for heavy stats
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[0]; // To be overridden
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return configService.getBoolean(configPath + ".enabled", true);
    }

    public int getViolationWeight() {
        return configService.getInt(configPath + ".violation_weight", 1);
    }

    public double getThreshold() {
        return configService.getDouble(configPath + ".threshold", 0.9);
    }

    @Override
    public void flag(UUID uuid, ACPlayerData data, double probability, String details) {
        if (!isEnabled()) return;
        
        // Add confidence to the player session
        data.addConfidence(probability);
        
        // Log locally if probability is high enough or if staff has verbose on
        if (probability >= getThreshold()) {
             data.addViolation(name, getViolationWeight());
             
             // Ensure staff notification happens on the main thread
             ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
             if (platform != null) {
                 platform.runTask(() -> {
                     String name = platform.getPlayerName(uuid);
                     String message = String.format("§6[Feat-AC] §e%s §ffailed §b%s §7(Prob: %.2f, %s)", 
                             name, getName(), probability, details);
                     
                     org.bukkit.Bukkit.getOnlinePlayers().stream()
                             .filter(p -> p.hasPermission("lifemod.anticheat.alerts"))
                             .filter(p -> fr.lampalon.lifemod.platform.bukkit.commands.impl.ACCommand.hasAlertsEnabled(p.getUniqueId()))
                             .forEach(p -> p.sendMessage(message));
                     
                     org.bukkit.Bukkit.getConsoleSender().sendMessage(message);
                 });
             }
        }
    }

    protected void notifyStaff(UUID uuid, ACPlayerData data, double probability, String details) {
        // Deprecated, logic moved to flag() for better control
    }
}
