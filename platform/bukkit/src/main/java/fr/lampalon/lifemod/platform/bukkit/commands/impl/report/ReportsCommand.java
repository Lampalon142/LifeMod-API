package fr.lampalon.lifemod.platform.bukkit.commands.impl.report;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.gui.ReportListGui;

public class ReportsCommand extends LifeCommand {
    public ReportsCommand() {
        super("reports", "lifemod.report.staff", true);
        setDescription("Open the reports management GUI.");
        setUsage("/reports");
    }

    @Override
    public void execute(CommandContext context) {
        new ReportListGui(context.getPlayer()).open();
    }
}
