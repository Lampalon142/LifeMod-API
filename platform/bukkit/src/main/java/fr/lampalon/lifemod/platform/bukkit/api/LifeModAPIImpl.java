package fr.lampalon.lifemod.platform.bukkit.api;

import fr.lampalon.lifemod.api.LifeModAPI;
import fr.lampalon.lifemod.api.antivpn.IPInfo;
import fr.lampalon.lifemod.api.antivpn.IVpnService;
import fr.lampalon.lifemod.api.chat.IChatService;
import fr.lampalon.lifemod.api.freeze.IFreezeService;
import fr.lampalon.lifemod.api.player.IPlayerService;
import fr.lampalon.lifemod.api.player.PlayerData;
import fr.lampalon.lifemod.api.sanction.ISanctionService;
import fr.lampalon.lifemod.api.sanction.Sanction;
import fr.lampalon.lifemod.api.sanction.SanctionType;
import fr.lampalon.lifemod.api.staff.IStaffModeService;
import fr.lampalon.lifemod.api.staff.IVanishService;
import fr.lampalon.lifemod.api.webhook.IWebhookService;
import fr.lampalon.lifemod.api.webhook.WebhookAuthor;
import fr.lampalon.lifemod.api.webhook.WebhookField;
import fr.lampalon.lifemod.api.webhook.WebhookFooter;
import fr.lampalon.lifemod.api.webhook.WebhookImage;
import fr.lampalon.lifemod.api.webhook.WebhookMessage;
import fr.lampalon.lifemod.api.webhook.WebhookThumbnail;
import fr.lampalon.lifemod.common.antivpn.AntiVPNService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LifeModAPIImpl implements LifeModAPI {

    private final LifeMod plugin;
    private final ISanctionService sanctionService;
    private final IVanishService vanishService;
    private final IVpnService vpnService;
    private final IFreezeService freezeService;
    private final IChatService chatService;
    private final IStaffModeService staffModeService;
    private final IWebhookService webhookService;
    private final IPlayerService playerService;

    public LifeModAPIImpl(LifeMod plugin) {
        this.plugin = plugin;
        this.sanctionService = new ApiSanctionService();
        this.vanishService = new ApiVanishService(plugin);
        this.vpnService = new ApiVpnService();
        this.freezeService = new ApiFreezeService(plugin);
        this.chatService = new ApiChatService(plugin);
        this.staffModeService = new ApiStaffModeService(plugin);
        this.webhookService = new ApiWebhookService();
        this.playerService = new ApiPlayerService(plugin);
    }

    @Override
    public ISanctionService getSanctionService() {
        return sanctionService;
    }

    @Override
    public IVanishService getVanishService() {
        return vanishService;
    }

    @Override
    public IVpnService getVpnService() {
        return vpnService;
    }

    @Override
    public IFreezeService getFreezeService() {
        return freezeService;
    }

    @Override
    public IChatService getChatService() {
        return chatService;
    }

    @Override
    public IStaffModeService getStaffModeService() {
        return staffModeService;
    }

    @Override
    public IWebhookService getWebhookService() {
        return webhookService;
    }

    @Override
    public IPlayerService getPlayerService() {
        return playerService;
    }

    // ─── Sanction adapter ─────────────────────────────────────────────────────

    private static class ApiSanctionService implements ISanctionService {
        private final fr.lampalon.lifemod.common.service.ISanctionService delegate =
                ServiceRegistry.get(fr.lampalon.lifemod.common.service.ISanctionService.class);

        @Override
        public CompletableFuture<Sanction> applySanction(Sanction sanction) {
            return delegate.applySanction(toInternal(sanction))
                    .thenApply(LifeModAPIImpl::toApi);
        }

        @Override
        public CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type,
                                                         UUID removedBy, String removedByName,
                                                         String reason, boolean silent) {
            return delegate.revokeSanction(playerUuid, toInternal(type),
                    removedBy, removedByName, reason, silent);
        }

        @Override
        public CompletableFuture<Sanction> getActiveSanction(UUID playerUuid, String playerName, SanctionType type) {
            return delegate.getActiveSanction(playerUuid, playerName, toInternal(type))
                    .thenApply(s -> s != null ? toApi(s) : null);
        }

        @Override
        public CompletableFuture<List<Sanction>> getHistory(UUID playerUuid) {
            return delegate.getHistory(playerUuid)
                    .thenApply(list -> list.stream().map(LifeModAPIImpl::toApi).collect(Collectors.toList()));
        }

        @Override
        public CompletableFuture<List<Sanction>> getSanctionsIssuedBy(String issuerName, UUID issuerUuid) {
            return delegate.getSanctionsIssuedBy(issuerName, issuerUuid)
                    .thenApply(list -> list.stream().map(LifeModAPIImpl::toApi).collect(Collectors.toList()));
        }

        @Override
        public CompletableFuture<Map<UUID, Sanction>> getActiveSanctions(Collection<UUID> playerUuids,
                                                                         String playerName, SanctionType type) {
            return delegate.getActiveSanctions(playerUuids, playerName, toInternal(type))
                    .thenApply(map -> map.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> toApi(e.getValue()))));
        }
    }

    // ─── Vanish adapter ───────────────────────────────────────────────────────

    private static class ApiVanishService implements IVanishService {
        private final LifeMod plugin;

        ApiVanishService(LifeMod plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean isVanished(UUID uuid) {
            return plugin.getVanishService().isVanished(uuid);
        }

        @Override
        public void setVanished(UUID uuid, boolean vanished, boolean silent) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getVanishService().setVanished(player, vanished, silent);
            }
        }

        @Override
        public Set<UUID> getVanishedPlayers() {
            return plugin.getVanishService().getVanishedPlayers();
        }
    }

    // ─── VPN adapter ──────────────────────────────────────────────────────────

    private static class ApiVpnService implements IVpnService {
        private final AntiVPNService delegate = ServiceRegistry.get(AntiVPNService.class);

        @Override
        public CompletableFuture<IPInfo> lookup(String ip) {
            if (delegate == null) return CompletableFuture.completedFuture(null);
            return delegate.getLookupManager().lookup(ip).thenApply(LifeModAPIImpl::toApi);
        }

        @Override
        public CompletableFuture<Boolean> shouldAllowConnection(String ip, String playerName) {
            if (delegate == null) return CompletableFuture.completedFuture(true);
            return delegate.shouldAllowConnection(ip, playerName);
        }

        @Override
        public CompletableFuture<Boolean> isProxy(String ip) {
            if (delegate == null) return CompletableFuture.completedFuture(false);
            return delegate.getLookupManager().lookup(ip).thenApply(info -> info != null && info.isProxy());
        }
    }

    // ─── Freeze adapter ───────────────────────────────────────────────────────

    private static class ApiFreezeService implements IFreezeService {
        private final LifeMod plugin;

        ApiFreezeService(LifeMod plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean isFrozen(UUID playerUuid) {
            return plugin.getFreezeManager().isPlayerFrozen(playerUuid);
        }

        @Override
        public void freeze(UUID moderatorUuid, UUID targetUuid) {
            Player moderator = Bukkit.getPlayer(moderatorUuid);
            Player target = Bukkit.getPlayer(targetUuid);
            if (moderator != null && target != null) {
                plugin.getFreezeManager().freezePlayer(moderator, target);
            }
        }

        @Override
        public void unfreeze(UUID moderatorUuid, UUID targetUuid) {
            Player moderator = Bukkit.getPlayer(moderatorUuid);
            Player target = Bukkit.getPlayer(targetUuid);
            if (moderator != null && target != null) {
                plugin.getFreezeManager().unfreezePlayer(moderator, target);
            }
        }

        @Override
        public Set<UUID> getFrozenPlayers() {
            return new HashSet<>(plugin.getFreezeManager().getFrozenPlayers().keySet());
        }
    }

    // ─── Chat adapter ─────────────────────────────────────────────────────────

    private static class ApiChatService implements IChatService {
        private final LifeMod plugin;

        ApiChatService(LifeMod plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean isEnabled() {
            return plugin.getChatManager().isEnabled();
        }

        @Override
        public void setEnabled(boolean enabled) {
            plugin.getChatManager().setEnabled(enabled);
        }

        @Override
        public List<String> getBlacklist() {
            return plugin.getChatManager().getBlacklist();
        }

        @Override
        public boolean addBlacklist(String word) {
            if (word == null || word.isEmpty()) return false;
            if (plugin.getChatManager().getBlacklist().contains(word)) return false;
            plugin.getChatManager().addToBlacklist(word);
            return true;
        }

        @Override
        public boolean removeBlacklist(String word) {
            if (!plugin.getChatManager().getBlacklist().contains(word)) return false;
            plugin.getChatManager().removeFromBlacklist(word);
            return true;
        }

        @Override
        public void awaitChatInput(UUID playerUuid, Consumer<String> callback) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                plugin.getChatManager().awaitChatInput(player, callback);
            }
        }
    }

    // ─── Staff mode adapter ───────────────────────────────────────────────────

    private static class ApiStaffModeService implements IStaffModeService {
        private final LifeMod plugin;

        ApiStaffModeService(LifeMod plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean isInStaffMode(UUID playerUuid) {
            Player player = Bukkit.getPlayer(playerUuid);
            return player != null && plugin.getStaffModeManager().isMod(player);
        }

        @Override
        public void enable(Player player) {
            plugin.getStaffModeManager().enableStaffMode(player);
        }

        @Override
        public void disable(Player player) {
            plugin.getStaffModeManager().disableStaffMode(player);
        }

        @Override
        public Set<UUID> getStaffPlayers() {
            return plugin.getStaffModeManager().getStaffPlayers();
        }
    }

    // ─── Webhook adapter ──────────────────────────────────────────────────────

    private static class ApiWebhookService implements IWebhookService {
        private final fr.lampalon.lifemod.common.service.IWebhookService delegate =
                ServiceRegistry.get(fr.lampalon.lifemod.common.service.IWebhookService.class);

        @Override
        public CompletableFuture<Void> send(WebhookMessage message) {
            if (delegate == null) return CompletableFuture.completedFuture(null);
            return delegate.send(toInternal(message));
        }

        @Override
        public boolean isEnabled() {
            return delegate != null && delegate.isEnabled();
        }
    }

    // ─── Player data adapter ──────────────────────────────────────────────────

    private static class ApiPlayerService implements IPlayerService {
        private final LifeMod plugin;

        ApiPlayerService(LifeMod plugin) {
            this.plugin = plugin;
        }

        @Override
        public CompletableFuture<PlayerData> getPlayerData(UUID playerUuid) {
            return CompletableFuture.supplyAsync(() -> {
                fr.lampalon.lifemod.common.model.PlayerData data =
                        plugin.getDatabaseManager().getDatabaseProvider().getPlayerData(playerUuid);
                return data != null ? toApi(data) : null;
            }, ServiceRegistry.get(ExecutorService.class));
        }
    }

    // ─── Model converters ─────────────────────────────────────────────────────

    private static fr.lampalon.lifemod.common.model.SanctionType toInternal(SanctionType type) {
        return fr.lampalon.lifemod.common.model.SanctionType.valueOf(type.name());
    }

    private static SanctionType toApi(fr.lampalon.lifemod.common.model.SanctionType type) {
        return SanctionType.valueOf(type.name());
    }

    private static fr.lampalon.lifemod.common.model.Sanction toInternal(Sanction s) {
        return new fr.lampalon.lifemod.common.model.Sanction(
                s.getUuid(), s.getPlayerUuid(), s.getPlayerName(),
                s.getIssuerUuid(), s.getIssuerName(),
                s.getServerName(), s.getCategory(),
                toInternal(s.getType()), s.getReason(),
                s.getCreatedAt(), s.getDuration(), s.isSilent(), true
        );
    }

    private static Sanction toApi(fr.lampalon.lifemod.common.model.Sanction s) {
        return new Sanction(
                s.getUuid(), s.getPlayerUuid(), s.getPlayerName(),
                s.getIssuerUuid(), s.getIssuerName(),
                s.getServerName(), s.getCategory(),
                toApi(s.getType()), s.getReason(),
                s.getCreatedAt(), s.getDuration(), s.isSilent(), s.isActive(),
                s.getEvidence(), s.getRemovedByUuid(), s.getRemovedByName(),
                s.getRemoveReason(), s.getRemovedAt()
        );
    }

    private static IPInfo toApi(fr.lampalon.lifemod.common.antivpn.data.IPInfo info) {
        if (info == null) return null;
        return new IPInfo(info.getIp(), info.getCountryCode(), info.getCountryName(),
                info.getIsp(), info.isProxy(), info.getLastUpdate());
    }

    private static PlayerData toApi(fr.lampalon.lifemod.common.model.PlayerData data) {
        return new PlayerData(data.getUuid(), data.getLastName(), data.getLastIp(),
                data.getLastSeen(), data.getFirstSeen(), data.getSessionCount(), data.isInStaffMode());
    }

    private static fr.lampalon.lifemod.common.model.webhook.WebhookMessage toInternal(WebhookMessage message) {
        fr.lampalon.lifemod.common.model.webhook.WebhookMessage.Builder builder =
                new fr.lampalon.lifemod.common.model.webhook.WebhookMessage.Builder();
        builder.setContent(message.getContent());
        builder.setUsername(message.getUsername());
        builder.setAvatarUrl(message.getAvatarUrl());
        builder.setTts(message.isTts());
        for (fr.lampalon.lifemod.api.webhook.WebhookEmbed embed : message.getEmbeds()) {
            builder.addEmbed(toInternal(embed));
        }
        return builder.build();
    }

    private static fr.lampalon.lifemod.common.model.webhook.WebhookEmbed toInternal(fr.lampalon.lifemod.api.webhook.WebhookEmbed embed) {
        fr.lampalon.lifemod.common.model.webhook.WebhookEmbed.Builder builder =
                new fr.lampalon.lifemod.common.model.webhook.WebhookEmbed.Builder();
        builder.setTitle(embed.getTitle());
        builder.setDescription(embed.getDescription());
        builder.setUrl(embed.getUrl());
        builder.setColor(embed.getColor());
        if (embed.getFooter() != null) {
            WebhookFooter footer = embed.getFooter();
            builder.setFooter(new fr.lampalon.lifemod.common.model.webhook.WebhookFooter(footer.text(), footer.iconUrl()));
        }
        if (embed.getThumbnail() != null) {
            WebhookThumbnail thumbnail = embed.getThumbnail();
            builder.setThumbnail(new fr.lampalon.lifemod.common.model.webhook.WebhookThumbnail(thumbnail.url()));
        }
        if (embed.getImage() != null) {
            WebhookImage image = embed.getImage();
            builder.setImage(new fr.lampalon.lifemod.common.model.webhook.WebhookImage(image.url()));
        }
        if (embed.getAuthor() != null) {
            WebhookAuthor author = embed.getAuthor();
            builder.setAuthor(new fr.lampalon.lifemod.common.model.webhook.WebhookAuthor(author.name(), author.url(), author.iconUrl()));
        }
        for (WebhookField field : embed.getFields()) {
            builder.addField(new fr.lampalon.lifemod.common.model.webhook.WebhookField(field.name(), field.value(), field.inline()));
        }
        return builder.build();
    }
}