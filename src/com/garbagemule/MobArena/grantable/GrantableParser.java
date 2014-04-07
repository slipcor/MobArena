package com.garbagemule.MobArena.grantable;

import java.util.NoSuchElementException;

public class GrantableParser {
    private String input;
    private int index;

    public GrantableParser(String input) {
        this.input = input.trim();
        this.index = 0;
    }

    public boolean hasNext() {
        return index < input.length();
    }

    public Grantable next() {
        // If we have no next element, throw an exception
        if (!hasNext()) {
            throw new NoSuchElementException("No more Grantables in input string");
        }

        // Otherwise, find the first non-space
        while (input.charAt(index) == ' ') {
            index++;
        }

        // Close parenthesis is an error
        if (input.charAt(index) == ')') {
            throw new IllegalArgumentException("Unmatched close parenthesis");
        }

        // Open parenthesis means GrantableGroup
        if (input.charAt(index) == '(') {
            // Find the close parenthesis
            int start = index;
            int end   = index + 1;
            while (input.charAt(end) != ')') {
                end++;
                if (end == input.length()) {
                    throw new IllegalArgumentException("Unmatched start parenthesis");
                }
            }

            // Advance the index past the next comma
            index = input.indexOf(',', end);
            if (index == -1) {
                index = input.length();
            }
            index++;

            // Grab the substring, then parse the group
            String sub = input.substring(start, end);
            return GrantableGroup.fromString(sub);
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

        // Then parse the Grantable
        if (s.startsWith("$")) return Currency.fromString(s);
        if (s.startsWith("#")) return Permission.fromString(s);
        if (s.startsWith("@")) return Effect.fromString(s);
        return Item.fromString(s);
    }
}
