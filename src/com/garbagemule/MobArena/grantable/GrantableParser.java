package com.garbagemule.MobArena.grantable;

import java.util.NoSuchElementException;

public class GrantableParser {
    private String input;
    private int index;

    public GrantableParser(String input) {
        this.input = input;
        this.index = 0;
    }

    public boolean hasNext() {
        return index < input.length();
    }

    public Grantable next() {
        // If we have no next element, throw
        if (!hasNext()) {
            throw new NoSuchElementException("No more Grantables in input string");
        }

        // Grab start and end pointers
        int start = index;
        int end   = input.indexOf(',', index);

        // Push end pointer to the end of the input if no more commas
        if (end == -1) {
            end = input.length();
        }

        // Update index
        index = end + 1;

        // Extract the string
        String s = input.substring(start, end).trim();
        if (s.length() == 0) {
            throw new IllegalArgumentException("The empty string is not a valid Grantable");
        }

        // Then parse the
        if (s.startsWith("$")) return Currency.fromString(s);
        if (s.startsWith("#")) return Permission.fromString(s);
        if (s.startsWith("@")) return Effect.fromString(s);
        return Item.fromString(s);
    }
}
