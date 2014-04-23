package com.garbagemule.MobArena.grantable;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GrantableGroup represents a composite Grantable, which is simply a
 * collection of other grantables, realizing the <i>Composite</i> role
 * in the classic Composite Pattern.
 */
public class GrantableGroup implements Grantable {
    private List<Grantable> elements;

    public GrantableGroup() {
        elements = new ArrayList<Grantable>(4);
    }

    @Override
    public boolean grant(Player player) {
        boolean result = true;
        for (Grantable element : elements) {
            result &= element.grant(player);
        }
        return result;
    }

    @Override
    public boolean take(Player player) {
        boolean result = true;
        for (Grantable element : elements) {
            result &= element.take(player);
        }
        return result;
    }

    @Override
    public boolean has(Player player) {
        for (Grantable element : elements) {
            if (!element.has(player)) {
                return false;
            }
        }
        return true;
    }

    public void add(Grantable element) {
        elements.add(element);
    }

    @Override
    public String toString() {
        // Prep the builder and iterator
        StringBuilder buffy = new StringBuilder("GrantableGroup[{");
        Iterator<Grantable> iter = elements.iterator();

        // First element should have no comma
        if (iter.hasNext()) {
            buffy.append(iter.next());
        }

        // But the rest should
        while (iter.hasNext()) {
            buffy.append(", ").append(iter.next());
        }

        // Append the end brace and return
        return buffy.append("}]").toString();
    }

    public static GrantableGroup fromString(String string) {
        // Trim off the parentheses, if they exist
        if (string.startsWith("(")) {
            string = string.substring(1).trim();
        }
        if (string.endsWith(")")) {
            string = string.substring(0, string.length() - 1).trim();
        }

        // Create the composite result and parse the input
        GrantableGroup group = new GrantableGroup();
        GrantableParser parser = new GrantableParser(string);
        while (parser.hasNext()) {
            group.add(parser.next());
        }
        return group;
    }
}
