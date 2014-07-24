package com.garbagemule.MobArena.grantable;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Effect represents a potion effect that can be applied to a player.
 */
public class Effect implements Grantable {
    private PotionEffectType type;
    private int amplifier;
    private int duration;

    /**
     * Create a new effect with the given potion effect, amplifier, and
     * duration.
     *
     * @param name name of a potion effect type
     * @param amplifier the potion amplifier/level
     * @param duration the effect duration (in ticks)
     * @throws IllegalArgumentException if the potion type does not exist
     */
    public Effect(String name, int amplifier, int duration) {
        this.amplifier = (amplifier > 0) ? amplifier : 0;
        this.duration  = (duration  > 0) ? duration  : Integer.MAX_VALUE;

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
        switch (amplifier) {
            case 0: level = "I";   break;
            case 1: level = "II";  break;
            case 2: level = "III"; break;
            case 3: level = "IV";  break;
            case 4: level = "V";   break;
            default: level = "" + amplifier + 1; break;
        }
        return type.getName() + " " + level;
    }

    /**
     * Convert a String into an Effect.
     * <p>
     * The syntax for an Effect is expressed in one of the following forms:
     * <pre>
     *     name/id
     *     name/id amp
     *     name/id duration
     *     name/id amp duration
     * </pre>
     * {@code name/id} is either an effect name ({@link PotionEffectType}) or
     * an effect ID, {@code amp} is either a roman numeral (from 1 to 5) or a
     * non-negative integer of at most two digits, and {@code duration} is a
     * time string denoting hours, minutes, and seconds, (e.g. {@code 2m30s})
     * or a normal non-negative integer.
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

        // Then split by space
        String[] parts = string.split(" ");

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

        // Then grab the amplifier/level and duration
        int amplifier;
        int duration;
        switch (parts.length) {
            // If we have 3 parts, amp before duration
            case 3:
                amplifier = parseAmplifier(parts[1]);
                duration  = parseDuration(parts[2]);
                break;
            // If we have 2 parts, assume amp if one or two digits
            case 2:
                // Pure roman numeral or 1-2 single digits -> amp
                if (parts[1].matches("[0-9][0-9]?|[IVX]+")) {
                    amplifier = parseAmplifier(parts[1]);
                    duration  = Integer.MAX_VALUE;
                } else {
                    amplifier = 0;
                    duration  = parseDuration(parts[1]);
                }
                break;
            // If we have 1 part, assume level 1 and infinite
            case 1:
                amplifier = 0;
                duration  = Integer.MAX_VALUE;
                break;
            // Too many parts, error
            default:
                throw new IllegalArgumentException("'" + string + "' is not valid effect syntax");
        }

        // Finally, return the potion effect
        return new Effect(name, amplifier, duration);
    }

    /**
     * Convert a string of the form {@code <number><unit>*} into an int equal
     * to the amount of ticks (20 per second) represented in the string.
     * <p>
     * {@code <number>} is a non-negative integer, and {@code <unit>} is one
     * of {@code sSmMhH} for seconds, minutes, and hours, respectively.
     * <p>
     * Note that if the string is just an integer, it is parsed as such.
     *
     * @param s a time string
     * @return the time in ticks represented by the time string
     */
    private static int parseDuration(String s) {
        if (s.matches("-?[1-9][0-9]*")) {
            return Integer.parseInt(s) * 20;
        }
        Matcher m = DURATION_PATTERN.matcher(s);
        int seconds = 0;
        while (m.find()) {
            int number = Integer.parseInt(m.group(1));
            switch (m.group(2).charAt(0)) {
                case 'h':
                case 'H':
                    seconds += (number * 3600);
                    break;
                case 'm':
                case 'M':
                    seconds += (number * 60);
                    break;
                case 's':
                case 'S':
                    seconds += number;
                    break;
                default: break;
            }
        }
        if (seconds == 0) {
            throw new IllegalArgumentException("'" + s + "' is not valid effect syntax");
        }
        return seconds * 20;
    }

    /**
     * Convert a Roman numeral (1-5) to its amplifier/level value.
     * <p>
     * Note that if the string is a non-positive integer instead of a Roman
     * numeral, it is parsed as a normal integer.
     *
     * @param s a Roman numeral or non-negative integer
     * @return the amplifier/level corresponding to the Roman numeral
     */
    private static int parseAmplifier(String s) {
        if (s.matches("[0-9]+")) {
            return Math.max(0, Integer.parseInt(s) - 1);
        }
        if (s.equals("I"))   return 0;
        if (s.equals("II"))  return 1;
        if (s.equals("III")) return 2;
        if (s.equals("IV"))  return 3;
        if (s.equals("V"))   return 4;
        throw new IllegalArgumentException("'" + s + "' is not a valid potion effect level");
    }

    /**
     * Pattern used by the {@link #parseDuration(String)} method.
     */
    private static final Pattern DURATION_PATTERN = Pattern.compile("([0-9]+)([sSmMhH])");
}
