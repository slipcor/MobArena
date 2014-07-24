package com.garbagemule.MobArena.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.garbagemule.MobArena.MAUtils;
import com.garbagemule.MobArena.Messenger;
import com.garbagemule.MobArena.grantable.GrantableParser;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.material.MaterialData;

public class ItemParser
{
    public static ItemStack parseItemStack(String s) {
        if (s != null && !s.equals("")) {
            try {
                return new GrantableParser(s).nextItem().toItemStack();
            } catch (Exception e) {
                Messenger.severe(e.getMessage());
            }
        }
        return null;
    }

    public static List<ItemStack> parseItemStacks(List<String> list) {
        return parseItemStacks(MAUtils.toString(list));
    }

    public static List<ItemStack> parseItemStacks(String s) {
        List<ItemStack> result = new ArrayList<ItemStack>();
        if (s != null && !s.equals("")) {
            try {
                GrantableParser parser = new GrantableParser(s);
                while (parser.hasNext()) {
                    try {
                        result.add(parser.nextItem().toItemStack());
                    } catch (IllegalArgumentException e) {
                        Messenger.severe(e.getMessage());
                    } catch (Exception e) {
                        Messenger.severe(e.getMessage());
                        break;
                    }
                }
            } catch (Exception e) {
                Messenger.severe(e.getMessage());
            }
        }
        return result;
    }
    
    public static String parseString(ItemStack... stacks) {
        String result = "";

        // Parse each stack
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getTypeId() == 0) continue;

            result += ", " + parseString(stack);
        }

        // Trim off the leading ', ' if it is there
        if (!result.equals("")) {
            result = result.substring(2);
        }

        return result;
    }

    public static String parseString(ItemStack stack) {
        if (stack.getTypeId() == 0) return null;

        // <item> part
        String type = stack.getType().toString().toLowerCase();

        // <data> part
        MaterialData md = stack.getData();
        short data = (md != null ? md.getData() : 0);

        // Take wool into account
        if (stack.getType() == Material.WOOL) {
            data = (byte) (15 - data);
        }

        // Take potions into account
        else if (stack.getType() == Material.POTION) {
            data = stack.getDurability();
        }

        // <amount> part
        int amount = stack.getAmount();

        // Enchantments
        Map<Enchantment,Integer> enchants = null;
        if (stack.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) stack.getItemMeta();
            enchants = esm.getStoredEnchants();
        } else {
            enchants = stack.getEnchantments();
        }
        String enchantments = "";
        for (Entry<Enchantment,Integer> entry : enchants.entrySet()) {
            int id  = entry.getKey().getId();
            int lvl = entry.getValue();

            // <eid>:<level>;
            enchantments += ";" + id + ":" + lvl;
        }

        // Trim off the leading ';' if it is there
        if (!enchantments.equals("")) {
            enchantments = enchantments.substring(1);
        }

        // <item>
        String result = type;

        // <item>(:<data>)
        if (data != 0) {
            result += ":" + data;
        }

        // <item>((:<data>):<amount>) - force if there is data
        if (amount > 1 || data != 0) {
            result += ":" + amount;
        }

        // <item>((:<data>):<amount>) (<eid>:<level>(;<eid>:<level>(; ... )))
        if (!enchantments.equals("")) {
            result += " " + enchantments;
        }

        return result;
    }
}
