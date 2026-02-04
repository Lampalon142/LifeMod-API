package fr.lampalon.lifemod.common.commands.framework;

public interface IPlayer extends ICommandSender {
    boolean getAllowFlight();
    void setAllowFlight(boolean allow);
    void setFlying(boolean flying);
}

