package com.garbagemule.MobArena.grantable;

import org.bukkit.entity.Player;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Effect represents a potion effect that can be applied to a player.
 */
public class Effect implements Grantable {
    private int amplifier;
    private int duration;
    private PotionEffectType type;

    public Effect(String name, int amplifier, int duration) {
        this.amplifier = amplifier;
        this.duration = (duration > 0) ? duration * 20 : Integer.MAX_VALUE;

        for (PotionEffectType type : PotionEffectType.values()) {
            // Apparently null is an actual value, so guard against it
            if (type == null) continue;

            if (name.equals(type.getName())) {
                this.type = type;
                return;
            }
        }
        throw new IllegalArgumentException("'" + name + "' is not a valid potion effect");
    }

    @Override
    public boolean grant(Player player) {
        PotionEffect effect = new PotionEffect(type, duration, amplifier);
        return player.addPotionEffect(effect);
    }

    @Override
    public boolean take(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().getName().equals(type.getName())) {
                player.removePotionEffect(effect.getType());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean has(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().getName().equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String level;
        switch (amplifier + 1) {
            case 1: level = "I";   break;
            case 2: level = "II";  break;
            case 3: level = "III"; break;
            case 4: level = "IV";  break;
            case 5: level = "V";   break;
            default: level = "" + amplifier + 1; break;
        }
        return type.getName() + " " + level;
    }

    /**
     * Convert a String into an Effect.
     *
     * @param string the string to convert
     * @return the resulting Effect
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the string is invalid
     */
    @GrantableInfo(symbol = '#', prefix = "eff(ect)")
    public static Effect fromString(String string) {
        // Guard against the empty string
        if (string.length() == 0) {
            throw new IllegalArgumentException("The empty string is not a valid effect");
        }

        // Then split by colon
        String[] parts = string.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("'" + string + "' is not valid effect syntax");
        }

        // Grab the effect name
        PotionEffectType type;
        if (parts[0].matches("[0-9]+")) {
            // If we're matching a number, convert and try to get by ID
            type = null;
            try {
                int id = Integer.parseInt(parts[0]);
                type = PotionEffectType.getById(id);
            } catch (NumberFormatException e) {
                // We'll throw an exception in the next line
            }
        } else {
            // Casing is handled by Bukkit
            type = PotionEffectType.getByName(parts[0]);
        }
        if (type == null) {
            throw new IllegalArgumentException("'" + parts[0] + "' is not a valid effect ID");
        }
        String name = type.getName();

        // Then the amplifier and duration
        int amp = Integer.MIN_VALUE;
        int duration = Integer.MIN_VALUE;
        try {
            amp = Math.max(0, Integer.parseInt(parts[1]) - 1);
            duration = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            // We'll throw exceptions in the next lines
        }
        if (amp == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("'" + parts[1] + "' is not a valid effect amplifier");
        }
        if (duration == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("'" + parts[2] + "' is not a valid effect duration");
        }

        // Finally, return the potion effect
        return new Effect(name, amp, duration);
    }
}
