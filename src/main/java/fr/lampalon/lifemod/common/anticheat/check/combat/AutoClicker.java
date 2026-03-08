package fr.lampalon.lifemod.common.anticheat.check.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * AutoClicker basique et robuste.
 * Calqué sur la logique du CPSListener pour une précision maximale.
 */
public class AutoClicker extends AbstractCheck {

    public AutoClicker(IConfigurationService configService) {
        super("AutoClicker", configService);
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{
                PacketType.Play.Client.ANIMATION
        };
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof PacketReceiveEvent)) return;
        PacketReceiveEvent event = (PacketReceiveEvent) context;

        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;

        long now = System.currentTimeMillis();
        ACPlayerData.FixedSizeQueue<Long> clicks = data.getClickIntervals();

        if (!clicks.isEmpty() && now - clicks.peekLast() < 5) {
            return;
        }
        clicks.addLast(now);

        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
            clicks.removeFirst();
        }

        int currentCPS = clicks.size();

        long sustainedStart = data.getHighCpsStartTime();
        long duration = 0;

        if (currentCPS >= 12) {
            if (sustainedStart == -1) {
                data.setHighCpsStartTime(now);
                sustainedStart = now;
            }
            duration = now - sustainedStart;

            if (duration > 5000) {
                double prob = Math.min(1.0, (duration - 5000) / 5000.0 + 0.5);
                flag(uuid, data, prob, "Sustained CPS (" + currentCPS + " for " + (duration/1000) + "s)");

                data.setHighCpsStartTime(now - 4000);
            }
        } else if (currentCPS < 8) {
            data.setHighCpsStartTime(-1);
        }

        int maxCps = configService.getInt("anticheat.checks.autoclicker.max_cps", 20);
        if (currentCPS > maxCps) {
            flag(uuid, data, 1.0, "CPS Limit: " + currentCPS);
            clicks.clear();
        }
    }
}