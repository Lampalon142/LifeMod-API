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
        if (!plugin.getConfigConfig().getBoolean("commands.enabled." + command.getName(), true)) {
            return;
        }

        commands.put(command.getName(), command);
        
        registerWithBukkit(command);
        for (String alias : command.getAliases()) {
            registerAlias(command, alias);
        }
    }

    private void registerWithBukkit(LifeCommand command) {
        org.bukkit.command.PluginCommand pluginCommand = plugin.getCommand(command.getName());
        if (pluginCommand != null) {
            BukkitCommandAdapter adapter = new BukkitCommandAdapter(command, plugin);
            pluginCommand.setExecutor(adapter);
            pluginCommand.setTabCompleter(adapter);
        } else if (commandMap != null) {
            BukkitCommandWrapper wrapper = new BukkitCommandWrapper(command, plugin);
            commandMap.register(plugin.getName(), wrapper);
        }
    }

    private void registerAlias(LifeCommand command, String alias) {
        org.bukkit.command.PluginCommand pluginAliasCommand = plugin.getCommand(alias);
        if (pluginAliasCommand != null) {
            BukkitCommandAdapter adapter = new BukkitCommandAdapter(command, plugin);
            pluginAliasCommand.setExecutor(adapter);
            pluginAliasCommand.setTabCompleter(adapter);
        }
    }

    public void scanAndRegisterCommands(String packageName) {
        try {
            String path = packageName.replace('.', '/');
            
            // Fix getFile() protected access via reflection
            java.lang.reflect.Method getFileMethod = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredMethod("getFile");
            getFileMethod.setAccessible(true);
            java.io.File file = (java.io.File) getFileMethod.invoke(plugin);
            
            JarFile jar = new JarFile(file);
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
                                constructor = clazz.getConstructor(LifeMod.class);
                                command = (LifeCommand) constructor.newInstance(plugin);
                            } catch (NoSuchMethodException e) {
                                constructor = clazz.getConstructor();
                                command = (LifeCommand) constructor.newInstance();
                            }
                            register(command);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to instantiate command class " + className + ": " + e.getMessage());
                    }
                }
            }
            jar.close();
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().severe("Error scanning commands in package " + packageName + ": " + e.getMessage());
        }
    }

    public Map<String, LifeCommand> getCommands() {
        return commands;
    }
}
