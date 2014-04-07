package com.garbagemule.MobArena.grantable;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

/**
 * Currency represents a Grantable in the form of economy money from various
 * economy plugins. The value of the currency is added to the player's balance
 * according to the protocol of Vault.
 */
public class Currency implements Grantable {
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
        // Cut off the dollar sign if it exists
        if (string.startsWith("$")) {
            string = string.substring(1);
        }

        // Convert to double and return
        try {
            double value = Double.parseDouble(string);
            return new Currency(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + string + "' is not a valid currency value");
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
