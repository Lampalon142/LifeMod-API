package fr.lampalon.lifemod.platform.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class InventoryUtil {

    public static byte[] serializeInventory(ItemStack[] contents, ItemStack[] armor) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataStream = new BukkitObjectOutputStream(outputStream)) {

            dataStream.writeInt(contents.length);
            for (ItemStack content : contents) {
                dataStream.writeObject(content);
            }

            dataStream.writeInt(armor.length);
            for (ItemStack item : armor) {
                dataStream.writeObject(item);
            }

            return outputStream.toByteArray();
        }
    }

    public static void deserializeInventory(Player player, byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             BukkitObjectInputStream dataStream = new BukkitObjectInputStream(inputStream)) {

            int contentSize = dataStream.readInt();
            ItemStack[] contents = new ItemStack[contentSize];
            for (int i = 0; i < contentSize; i++) {
                contents[i] = (ItemStack) dataStream.readObject();
            }
            player.getInventory().setContents(contents);

            int armorSize = dataStream.readInt();
            ItemStack[] armor = new ItemStack[armorSize];
            for (int i = 0; i < armorSize; i++) {
                armor[i] = (ItemStack) dataStream.readObject();
            }
            player.getInventory().setArmorContents(armor);
        }
    }
}
