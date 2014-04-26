package com.garbagemule.MobArena.grantable;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return perm != null && perm.playerAdd(player, permission);
    }

    @Override
    public boolean take(Player player) {
        return perm != null && perm.playerRemove(player, permission);
    }

    @Override
    public boolean has(Player player) {
        return perm != null && perm.playerHas(player, permission);
    }

    @Override
    public String toString() {
        return "Permission[" + permission + "]";
    }

    /**
     * Convert a String into a Permission.
     *
     * @param string the string to convert
     * @return the resulting Permission
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the permission string is invalid
     */
    @GrantableInfo(symbol = '%', prefix = "perm(ission)")
    public static Permission fromString(String string) {
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
    public static void setPerm(net.milkbowl.vault.permission.Permission perm) {
        Permission.perm = perm;
    }
}
