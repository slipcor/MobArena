package com.garbagemule.MobArena.grantable;

import org.bukkit.entity.Player;

/**
 * Grantables are anything that can be granted to (or taken from) players.
 * This includes, primarily, inventory items, but also permissions and money
 * (from economy plugins), and anything else that can be granted or taken
 * away from players.
 * <p>
 * The purpose of this interface is to create a unifying umbrella for
 * everything that can be granted or taken away by MobArena, to avoid having
 * to create dirty hacks for things like money, and to avoid parameterized
 * solutions to support things like permissions.
 * <p>
 * The interface provides methods for granting and taking the Grantable, as
 * well as a method for checking whether or not a player has the Grantable
 * or an equivalent.
 */
public interface Grantable {
    /**
     * Grant the given player this Grantable.
     *
     * @param player the player to grant this Grantable to
     * @return true, if the player was successfully granted this Grantable,
     * false otherwise
     */
    public boolean grant(Player player);

    /**
     * Take a Grantable equivalent to this Grantable from the player.
     *
     * @param player the player to take this Grantable from
     * @return true, if this Grantable was successfully taken from the
     * player, false otherwise
     */
    public boolean take(Player player);

    /**
     * Check if the player has this Grantable.
     *
     * @param player the player to check
     * @return true, if the player has this Grantable, or an equivalent,
     * false otherwise
     */
    public boolean has(Player player);
}
