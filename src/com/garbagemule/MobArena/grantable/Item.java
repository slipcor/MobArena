package com.garbagemule.MobArena.grantable;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An Item is an inventory item represented by a set of pure Java types,
 * rather than an implementation-specific type, such as Bukkit's ItemStack,
 * for the purpose of easy serialization.
 * <p>
 * The class provides static methods {@link #fromItemStack(ItemStack)} and
 * {@link #fromString(String)} to parse Item instances from ItemStacks and
 * strings (from config-files, usually), respectively. It also provides the
 * {@link #toItemStack()} method to convert itself to an ItemStack, ready
 * to be granted to a player ({@link #grant(Player)} uses this method
 * internally).
 */
public class Item implements Grantable {
    private final int id;
    private final int amount;
    private final short data;
    private Map<String,Integer> enchantments;

    public Item(int id, int amount, short data) {
        this.id = id;
        this.amount = amount;
        this.data = data;
    }

    @Override
    public boolean grant(Player player) {
        PlayerInventory inv = player.getInventory();
        Map<Integer,ItemStack> result = inv.addItem(toItemStack());
        return result.isEmpty();
    }

    @Override
    public boolean take(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        int remaining = amount;
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null || contents[i].getTypeId() != id) {
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

        int remaining = amount;
        for (ItemStack item : contents) {
            if (item == null || item.getTypeId() != id) {
                continue;
            }
            remaining -= item.getAmount();
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public void addEnchantment(String enchantment, int level) {
        if (enchantments == null) {
            enchantments = new HashMap<String,Integer>();
        }
        enchantments.put(enchantment, level);
    }

    /**
     * Convert this Item to an ItemStack
     *
     * @return this Item as an ItemStack
     */
    public ItemStack toItemStack() {
        // Create the ItemStack
        ItemStack stack = new ItemStack(id, amount, data);

        // Add enchantments, if we have any
        if (enchantments != null) {
            for (Entry<String,Integer> entry : enchantments.entrySet()) {
                Enchantment ench = Enchantment.getByName(entry.getKey());
                stack.addUnsafeEnchantment(ench, entry.getValue());
            }
        }
        return stack;
    }

    @Override
    public String toString() {
        return Material.getMaterial(id).toString().toLowerCase() + (amount == 1 ? "" : " x" + amount);
    }

    /**
     * Convert an ItemStack into an Item.
     * <p>
     * Note that this methods discards item meta data!
     *
     * @param stack the ItemStack to convert
     * @return the resulting Item
     * @throws NullPointerException if stack is null
     */
    public static Item fromItemStack(ItemStack stack) {
        // Create the Item from the ItemStack data
        Item item = new Item(stack.getTypeId(), stack.getAmount(), dataOf(stack));

        // Add enchantments, if any
        for (Entry<Enchantment,Integer> entry : stack.getEnchantments().entrySet()) {
            item.addEnchantment(entry.getKey().getName(), entry.getValue());
        }
        return item;
    }

    private static short dataOf(ItemStack stack) {
        // Potions just use durability instead of MaterialData
        if (stack.getType() == Material.POTION) {
            return stack.getDurability();
        }

        // Otherwise, grab the MaterialData and its byte value
        MaterialData md = stack.getData();
        short data = (md != null) ? md.getData() : 0;

        // Wool is a bit tricky, as it inverts the byte value
        if (stack.getType() == Material.WOOL) {
            data = (byte) (15 - data);
        }
        return data;
    }

    /**
     * Convert a String into an Item.
     * <p>
     * This method is the main item parser of MobArena, and it is specified by
     * the following BNF ({@code EMPTY} is the empty string, {@code SPACE} is
     * a space, {@code COLON} is a colon, and {@code NUMBER} is an integer):
     * <pre>
     *     # Non-terminals
     *     item             ::=  id_or_name opt_amount_data opt_enchantments
     *
     *     id_or_name       ::=  ITEM_ID | ITEM_NAME
     *
     *     opt_amount_data  ::=  EMPTY | amount | data_amount
     *     amount           ::=  COLON NUMBER
     *     data_amount      ::=  COLON NUMBER COLON NUMBER
     *
     *     opt_enchantments ::=  EMPTY | SPACE enchantments
     *     enchantments     ::=  ench | ench SEMICOLON enchantments
     *     ench             ::=  ENCH_ID COLON NUMBER
     *
     *     # Literals:
     *     ITEM_ID          ::=  an item ID
     *     ITEM_NAME        ::=  an item name according to {@link org.bukkit.Material}
     *     ENCH_ID          ::=  an enchantment ID
     * </pre>
     *
     * @param string the string to convert
     * @return the resulting Item
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the string is invalid
     */
    public static Item fromString(String string) {
        // Grab the base, then add enchants
        Item item = baseOf(string);
        Map<String,Integer> enchants = enchantsOf(string);
        for (Entry<String,Integer> entry : enchants.entrySet()) {
            item.addEnchantment(entry.getKey(), entry.getValue());
        }
        return item;
    }

    private static Item baseOf(String string) {
        // Split by space, then by colon
        String[] parts = string.trim().split(" ");
        String item = parts[0];
        parts = item.split(":");

        // Item variables
        int id = -1;
        int amount = 1;
        short data = 0;

        /* We switch on the length of the item part so we can capture the
         * three different cases of id/name only, id/name with amount, and
         * id/name with data and amount. The switch-statement is designed
         * to intentionally fall through, and in case of a data value being
         * present, it has to be overwritten by the amount at the end of
         * parsing the data value. Thus, after this switch-statement, the
         * contents of the parts array may be inconsistent! */
        switch (parts.length) {
            case 3:
                if (parts[1].matches("[0-9]+")) {
                    // If we're matching a number, just parse it as a byte
                    try {
                        data = Short.parseShort(parts[1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("'" + parts[1] + "' is not a valid data value");
                    }
                } else {
                    // Otherwise, try to parse the color name
                    try {
                        DyeColor color = DyeColor.valueOf(parts[1].toUpperCase());
                        data = color.getDyeData();
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("'" + parts[1] + "' is not a valid color");
                    }
                }
                // Overwrite data with amount for intentional fall-through
                parts[1] = parts[2];
            case 2:
                try {
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("'" + parts[1] + "' is not a valid amount");
                }
                // Intentional fall-through
            case 1:
                if (parts[0].matches("[0-9]+")) {
                    try {
                        id = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("'" + parts[0] + "' is not a valid item ID");
                    }
                } else {
                    Material mat = Material.matchMaterial(parts[0]);
                    if (mat == null) {
                        throw new IllegalArgumentException("'" + parts[0] + "' is not a valid item name");
                    }
                    id = mat.getId();
                }
                break;
            default:
                throw new IllegalArgumentException("'" + string + "' is not valid item syntax");
        }

        // Finally, create and return the item
        return new Item(id, amount, data);
    }

    private static Map<String,Integer> enchantsOf(String string) {
        // Split by space, bail if we have no enchants part
        String[] parts = string.trim().split(" ");
        if (parts.length < 2) {
            return Collections.emptyMap();
        }

        // Otherwise, split by semicolon
        parts = parts[1].split(";");

        // Then grab all the enchants
        Map<String,Integer> result = new HashMap<String,Integer>();
        for (String part : parts) {
            // Require <enchantment>:<level> syntax
            String[] enchantment = part.split(":");
            if (enchantment.length != 2) {
                throw new IllegalArgumentException("'" + part + "' is not valid enchantment syntax");
            }
            Enchantment ench;
            if (enchantment[0].matches("[0-9]+")) {
                // If we're matching a number, just parse the ID as an int
                try {
                    int id = Integer.parseInt(enchantment[0]);
                    ench = Enchantment.getById(id);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("'" + enchantment[0] + "' is not a valid enchantment ID");
                }
            } else {
                // Otherwise, grab by name
                ench = Enchantment.getByName(enchantment[0]);
            }

            // If the enchantment is invalid, throw
            if (ench == null) {
                throw new IllegalArgumentException("'" + enchantment[0] + "' is not a valid enchantment ID");
            }

            // Otherwise, parse the level and add
            try {
                int lvl = Integer.parseInt(enchantment[1]);
                result.put(ench.getName(), lvl);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + enchantment[1] + "' is not a valid enchantment level");
            }
        }
        return result;
    }
}
