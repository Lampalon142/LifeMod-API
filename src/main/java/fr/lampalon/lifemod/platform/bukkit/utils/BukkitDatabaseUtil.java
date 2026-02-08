package fr.lampalon.lifemod.platform.bukkit.utils;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BukkitDatabaseUtil {

    public static byte[] serializeInventory(Inventory inventory) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream(); 
             BukkitObjectOutputStream o = new BukkitObjectOutputStream(b)) {
            o.writeInt(inventory.getSize());
            for (ItemStack i : inventory.getContents()) {
                o.writeObject(i);
            }
            return b.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack[] deserializeInventory(byte[] data) {
        if (data == null) return null;
        try (ByteArrayInputStream b = new ByteArrayInputStream(data); 
             BukkitObjectInputStream i = new BukkitObjectInputStream(b)) {
            int size = i.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int x = 0; x < size; x++) {
                items[x] = (ItemStack) i.readObject();
            }
            return items;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static DatabaseProvider.StoredLocation toStoredLocation(Location loc) {
        if (loc == null) return null;
        return new DatabaseProvider.StoredLocation(
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        );
    }

    public static Location fromStoredLocation(DatabaseProvider.StoredLocation sl) {
        if (sl == null) return null;
        return new Location(
                Bukkit.getWorld(sl.world),
                sl.x, sl.y, sl.z,
                sl.yaw, sl.pitch
        );
    }
}
