package fr.lampalon.lifemod.platform.bukkit.commands.engine;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CommandRegistry {
    private final LifeMod plugin;
    private CommandMap commandMap;
    private final Map<String, LifeCommand> commands = new HashMap<>();

    public CommandRegistry(LifeMod plugin) {
        this.plugin = plugin;
        setupCommandMap();
    }

    private void setupCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            this.commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().severe("Could not access commandMap: " + e.getMessage());
        }
    }

    public void register(LifeCommand command) {
        // Check if enabled in config
        if (!plugin.getConfigConfig().getBoolean("commands.enabled." + command.getName(), true)) {
            plugin.getLogger().info("Command '" + command.getName() + "' is disabled in config.");
            return;
        }

        commands.put(command.getName(), command);
        
        org.bukkit.command.PluginCommand pluginCommand = plugin.getCommand(command.getName());
        if (pluginCommand != null) {
            // If command is defined in plugin.yml, use an adapter to bridge
            BukkitCommandWrapper wrapper = new BukkitCommandWrapper(command, plugin);
            pluginCommand.setExecutor(wrapper);
            pluginCommand.setTabCompleter(wrapper);
            plugin.getLogger().info("Registered command '" + command.getName() + "' via plugin.yml.");
        } else if (commandMap != null) {
            // Dynamic registration
            BukkitCommandWrapper wrapper = new BukkitCommandWrapper(command, plugin);
            commandMap.register(plugin.getName(), wrapper);
            plugin.getLogger().info("Dynamically registered command '" + command.getName() + "'.");
        } else {
            plugin.getLogger().warning("Could not register command: " + command.getName() + " (not in plugin.yml and commandMap is null)");
        }
    }

    public void scanAndRegisterCommands(String packageName) {
        try {
            String path = packageName.replace('.', '/');
            JarFile jar = new JarFile(plugin.getFile());
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(path) && name.endsWith(".class")) {
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (LifeCommand.class.isAssignableFrom(clazz) && !clazz.isInterface() && !clazz.isAnonymousClass() && !clazz.isMemberClass()) {
                            Constructor<?> constructor;
                            LifeCommand command;
                            try {
                                // Try constructor with LifeMod plugin
                                constructor = clazz.getConstructor(LifeMod.class);
                                command = (LifeCommand) constructor.newInstance(plugin);
                            } catch (NoSuchMethodException e) {
                                // Fallback to no-arg constructor
                                constructor = clazz.getConstructor();
                                command = (LifeCommand) constructor.newInstance();
                            }
                            register(command);
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        plugin.getLogger().warning("Failed to instantiate command class " + className + ": " + e.getMessage());
                    }
                }
            }
            jar.close();
        } catch (IOException e) {
            plugin.getLogger().severe("Error scanning commands in package " + packageName + ": " + e.getMessage());
        }
    }

    public Map<String, LifeCommand> getCommands() {
        return commands;
    }
}
