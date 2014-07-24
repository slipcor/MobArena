package com.garbagemule.MobArena.grantable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/**
 * A BukkitItem is quite simply a grantable ItemStack. Objects wrap ItemStack
 * instances and the {@link #grant(Player)} method simply adds the underlying
 * ItemStack to the player's current inventory, while {@link #take(Player)}
 * removes it.
 */
public class BukkitItem implements Grantable {
    private ItemStack stack;

    public BukkitItem(ItemStack stack) {
        this.stack = stack.clone();
    }

    @Override
    public boolean grant(Player player) {
        PlayerInventory inv = player.getInventory();
        Map<Integer,ItemStack> result = inv.addItem(stack);
        return result.isEmpty();
    }

    @Override
    public boolean take(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        int remaining = stack.getAmount();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null || contents[i].getType() != stack.getType()) {
                continue;
            }

            int amount = contents[i].getAmount();
            if (amount > remaining) {
                contents[i].setAmount(amount - remaining);
                remaining = 0;
            } else {
                contents[i] = null;
                remaining -= amount;
            }

            if (remaining == 0) {
                inv.setContents(contents);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean has(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        int remaining = stack.getAmount();
        for (ItemStack item : contents) {
            if (item == null || item.getType() != stack.getType()) {
                continue;
            }
            remaining -= item.getAmount();
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String amount = stack.getAmount() == 1 ? "" : " x" + stack.getAmount();
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            String name = meta.getDisplayName();
            if (name != null && !name.equals("")) {
                return name + amount;
            }
        }
        return stack.getType().toString().toLowerCase() + amount;
    }
}
