package fr.lampalon.lifemod.nms;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class TitleBuilder {

    private Component title;
    private Component subtitle;
    private int fadeIn = 10;
    private int stay = 70;
    private int fadeOut = 20;

    private TitleBuilder() {
    }

    public static TitleBuilder of() {
        return new TitleBuilder();
    }

    public TitleBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public TitleBuilder subtitle(Component subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public TitleBuilder fadeIn(int ticks) {
        this.fadeIn = ticks;
        return this;
    }

    public TitleBuilder stay(int ticks) {
        this.stay = ticks;
        return this;
    }

    public TitleBuilder fadeOut(int ticks) {
        this.fadeOut = ticks;
        return this;
    }

    public void send(Player player) {
        WrapperPlayServerTitle timePacket = new WrapperPlayServerTitle(
                WrapperPlayServerTitle.TitleAction.SET_TIMES_AND_DISPLAY,
                Component.empty(), Component.empty(), Component.empty(),
                fadeIn, stay, fadeOut
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, timePacket);

        if (title != null) {
            WrapperPlayServerTitle titlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_TITLE,
                    title, Component.empty(), Component.empty(),
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, titlePacket);
        }

        if (subtitle != null) {
            WrapperPlayServerTitle subtitlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_SUBTITLE,
                    Component.empty(), subtitle, Component.empty(),
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, subtitlePacket);
        }
    }
}
