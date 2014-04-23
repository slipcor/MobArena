package com.garbagemule.MobArena.grantable;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;

public class GrantableParser {
    private String input;
    private Queue<String> tokens;

    public GrantableParser(String input) {
        if (input == null) {
            throw new NullPointerException("Input cannot be null");
        }
        this.input = input.trim();
        this.tokens = new ArrayDeque<String>();
        tokenize();
    }

    /**
     * Tokenize the input string by splitting it by commas.
     * <p>
     * In each iteration of the tokenizer loop, the character at the current
     * position in the input string is evaluated. An open parenthesis denotes
     * a group, in which case the matching close parenthesis is found, and
     * the entire string is extracted as one token regardless of the amount
     * of commas between the parentheses. For non-group items, the next comma
     * is found and the entire string up to the comma becomes the token. The
     * loop finishes by skipping ahead to the next non-space character in the
     * input string.
     * <p>
     * Tokens are stored in a FIFO queue (empty strings are skipped) for later
     * parsing via the {@link #next()} methods.
     */
    private void tokenize() {
        String token;
        int pos = 0;

        while (pos < input.length()) {
            // Reset token for group/non-group check
            token = null;

            // A lone right paren is an error
            if (input.charAt(pos) == ')') {
                throw new IllegalArgumentException("Unmatched close parenthesis");
            }

            // A left parent is a group
            if (input.charAt(pos) == '(') {
                // Find right paren
                int close = input.indexOf(')', pos + 1);
                if (close == -1) {
                    throw new IllegalArgumentException("Unmatched start parenthesis");
                }

                // Extract the group (including parens)
                close++;
                token = input.substring(pos, close);
                pos = close;
            }

            // Find the index of the comma or end-of-input
            int comma = input.indexOf(',', pos);
            if (comma == -1) {
                comma = input.length();
            }

            // If we already have a token, it's a group, so don't do anything
            if (token == null) {
                token = input.substring(pos, comma).trim();
            }

            // Only add non-empty strings
            if (token.length() > 0) {
                tokens.add(token);
            }

            // Advance pos past the comma to the next non-space
            pos = comma + 1;
            while (pos < input.length() && input.charAt(pos) == ' ') {
                pos++;
            }
        }
    }

    /**
     * Check if there are any more tokens in the parser.
     *
     * @return true, if the parser has more tokens, false otherwise
     */
    public boolean hasNext() {
        return !tokens.isEmpty();
    }

    /**
     * Parse and return the next Grantable from the input string.
     *
     * @return the next Grantable, if one exists
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException on illegal token formats
     */
    public Grantable next() {
        ensureNext();
        String s = tokens.poll();

        // Groups are a special case
        if (s.startsWith("(")) {
            return GrantableGroup.fromString(s);
        }

        // Parse the Grantable
        if (Currency.PATTERN.matcher(s).matches())   return Currency.fromString(s);
        if (Permission.PATTERN.matcher(s).matches()) return Permission.fromString(s);
        if (Effect.PATTERN.matcher(s).matches())     return Effect.fromString(s);
        return Item.fromString(s);
    }

    /**
     * Parse the next Grantable in the input string as an Item.
     *
     * @return the next Grantable as an Item
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid
     * Item syntax
     */
    public Item nextItem() {
        ensureNext();
        return Item.fromString(tokens.poll());
    }

    /**
     * Parse the next Grantable in the input string as a Currency.
     *
     * @return the next Grantable as a Currency
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid
     * Currency syntax
     */
    public Currency nextCurrency() {
        ensureNext();
        return Currency.fromString(tokens.poll());
    }

    /**
     * Parse the next Grantable in the input string as a Permission.
     *
     * @return the next Grantable as a Permission
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid
     * Permission syntax
     */
    public Permission nextPermission() {
        ensureNext();
        return Permission.fromString(tokens.poll());
    }

    /**
     * Parse the next Grantable in the input string as an Effect.
     *
     * @return the next Grantable as an Effect
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid
     * Effect syntax
     */
    public Effect nextEffect() {
        ensureNext();
        return Effect.fromString(tokens.poll());
    }

    /**
     * Parse the next Grantable in the input string as a custom type.
     * <p>
     * Note that this method simply calls {@link #nextCustom(Class, String)}
     * with {@code fromString} as the method name.
     *
     * @param clazz the custom type
     * @return the next Grantable as a custom type
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid
     */
    public <T extends Grantable> T nextCustom(Class<T> clazz) {
        return nextCustom(clazz, "fromString");
    }

    /**
     * Parse the next Grantable in the input string as a custom type.
     *
     * @param clazz the custom type
     * @param method the name of the method to call
     * @return the next Grantable as a custom type
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid
     */
    @SuppressWarnings("unchecked")
    public <T extends Grantable> T nextCustom(Class<T> clazz, String method) {
        ensureNext();

        Method m;
        try {
            m = clazz.getDeclaredMethod(method, String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Method " + clazz.getName() + "." + method + "(String) not found");
        }

        String token = tokens.poll();
        Object result;
        try {
            result = m.invoke(null, token);
        } catch (ReflectiveOperationException e) {
            // Re-throw the exception to avoid reflection wrapping, if possible
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            // Otherwise, just wrap in an IllegalArgumentException
            throw new IllegalArgumentException(e.getCause());
        }
        return (T) result;
    }

    /**
     * Ensure that there are more tokens by throwing an exception if there
     * aren't.
     *
     * @throws NoSuchElementException if there are no more tokens
     */
    private void ensureNext() {
        // If we have no next element, throw an exception
        if (!hasNext()) {
            throw new NoSuchElementException("No more tokens in input");
        }
    }
}
