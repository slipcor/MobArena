package com.garbagemule.MobArena.grantable;

import org.bukkit.entity.Player;

public class Permission implements Grantable {
    /**
     * The Vault-Permission instance, initialized by MobArena
     */
    private static net.milkbowl.vault.permission.Permission perm = null;

    private String permission;

    public Permission(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean grant(Player player) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean take(Player player) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean has(Player player) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Convert a String into a Permission.
     * <p>
     * The format is simply {@code #<perm>}, where {@code <perm>} is a
     * permission string, e.g. {@code mobarena.use.leave}
     *
     * @param string the string to convert
     * @return the resulting Permission
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the permission string is invalid
     */
    public static Permission fromString(String string) {
        // Cut off the hashtag if it exists
        if (string.startsWith("#")) {
            string = string.substring(1);
        }

        // Guard against the empty string
        if (string.length() == 0) {
            throw new IllegalArgumentException("The empty string is not a valid permission");
        }
        return new Permission(string);
    }

    /**
     * Set the current Vault-Permission instance to use for Permission grantables.
     *
     * @param perm a Vault-Permission instance
     */
    public static void setEconomy(net.milkbowl.vault.permission.Permission perm) {
        Permission.perm = perm;
    }
}
