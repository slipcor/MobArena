package com.garbagemule.MobArena.grantable;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Currency represents a Grantable in the form of economy money from various
 * economy plugins. The value of the currency is added to the player's balance
 * according to the protocol of Vault.
 */
public class Currency implements Grantable {
    /**
     * Parser pattern. Examples:
     * <ul>
     *     <li>$3.14</li>
     *     <li>eco:3.14</li>
     *     <li>economy:3.14</li>
     * </ul>
     * The first matcher group is the prefix, and the second group is the
     * currency string
     */
    public static final Pattern PATTERN = Pattern.compile("(\\$|(?:eco(?:nomy)?):)([^\\s]+)");

    /**
     * The Vault-Economy instance, initialized by MobArena
     */
    private static net.milkbowl.vault.economy.Economy eco = null;

    private double value;

    public Currency(double value) {
        this.value = value;
    }

    @Override
    public boolean grant(Player player) {
        if (eco == null) {
            return false;
        }
        EconomyResponse response = eco.depositPlayer(player.getName(), value);
        return response.type == EconomyResponse.ResponseType.SUCCESS;
    }

    @Override
    public boolean take(Player player) {
        if (eco == null) {
            return false;
        }
        EconomyResponse response = eco.withdrawPlayer(player.getName(), value);
        return response.type == EconomyResponse.ResponseType.SUCCESS;
    }

    @Override
    public boolean has(Player player) {
        return eco != null && eco.has(player.getName(), value);
    }

    @Override
    public String toString() {
        return "Currency[$" + value + "]";
    }

    /**
     * Convert a String into a Currency.
     * <p>
     * The format is simply {@code $<value>}, where {@code <value>} is a
     * decimal value, e.g. 3.14 or 500.
     *
     * @param string the string to convert
     * @return the resulting Currency
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the string is invalid
     */
    public static Currency fromString(String string) {
        // Isolate the actual currency string
        Matcher matcher = PATTERN.matcher(string);
        if (matcher.find()) {
            string = matcher.group(2);
        }

        // Guard against the empty string
        if (string.length() == 0) {
            throw new IllegalArgumentException("The empty string is not a valid currency");
        }

        // Convert to double and return
        try {
            double value = Double.parseDouble(string);
            return new Currency(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + string + "' is not a valid currency");
        }
    }

    /**
     * Set the current Vault-Economy instance to use for Currency grantables.
     *
     * @param eco an Economy instance
     */
    public static void setEco(net.milkbowl.vault.economy.Economy eco) {
        Currency.eco = eco;
    }
}
