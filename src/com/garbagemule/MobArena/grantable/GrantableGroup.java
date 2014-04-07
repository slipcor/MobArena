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
        for (Grantable element : elements) {
            if (!element.grant(player)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean take(Player player) {
        for (Grantable element : elements) {
            if (!element.take(player)) {
                return false;
            }
        }
        return true;
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

    public static GrantableGroup fromString(String string) {
        // Trim off the start parenthesis, if it exists
        if (string.startsWith("(")) {
            string = string.substring(1).trim();
        }

        // As well as the end parenthesis
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
