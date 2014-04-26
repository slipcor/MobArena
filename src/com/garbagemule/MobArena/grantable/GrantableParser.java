package com.garbagemule.MobArena.grantable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrantableParser {
    private static Map<Class<? extends Grantable>, RegisteredGrantable> core, custom;
    static {
        core = new HashMap<Class<? extends Grantable>, RegisteredGrantable>();
        core.put(Currency.class, new RegisteredGrantable(Currency.class));
        core.put(Permission.class, new RegisteredGrantable(Permission.class));
        core.put(Effect.class, new RegisteredGrantable(Effect.class));
        // Item is in the core, but it is the default/fallback type

        custom = null;
    }

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

        // Custom Grantables go first
        if (custom != null) {
            for (RegisteredGrantable reg : custom.values()) {
                if (reg.matches(s)) {
                    return reg.parse(s);
                }
            }
        }

        // Then the core Grantables
        for (RegisteredGrantable reg : core.values()) {
            if (reg.matches(s)) {
                return reg.parse(s);
            }
        }

        // Finally, if nothing else matches, fall back to Item
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
        String token = trim(Currency.class, tokens.poll());
        return Currency.fromString(token);
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
        String token = trim(Permission.class, tokens.poll());
        return Permission.fromString(token);
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
        String token = trim(Effect.class, tokens.poll());
        return Effect.fromString(token);
    }

    /**
     * Parse the next Grantable in the input string as a custom type.
     * <p>
     * If the custom type has not been registered in the parser, this method
     * simply calls {@link #next(Class, String)} with {@code fromString} as
     * the method name, and thus, the class must have a static method called
     * {@code fromString} that takes a String as its only argument and returns
     * an instance of the class parameter.
     *
     * @param type the custom type
     * @return the next Grantable as a custom type
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid, or, if
     * the type has not been registered, if a {@code fromString} method was not
     * found, or if the method is non-static, has an incorrect parameter list
     * or an incorrect return type
     */
    public <T extends Grantable> T next(Class<T> type) {
        RegisteredGrantable reg = core.get(type);
        if (reg != null || (custom != null && (reg = custom.get(type)) != null)) {
            ensureNext();
            Grantable res = reg.parse(tokens.poll());
            return type.cast(res);
        }
        return next(type, "fromString");
    }

    /**
     * Parse the next Grantable in the input string as a custom type.
     * <p>
     * Note that this method simply calls {@link #next(Class, Method)} after
     * fetching the {@link Method} with the given name, and thus, the class
     * must have a static method with that name that takes a String as its
     * only argument and returns an instance of the class parameter.
     *
     * @param type the custom type
     * @param method the name of the method to call; the method must be static,
     * it must take a single parameter of type {@link String}, and it must
     * return an instance of the class parameter (or a subclass)
     * @return the next Grantable as a custom type
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid, or if
     * the method was not found, is non-static, or has an incorrect parameter
     * list or an incorrect return type
     */
    public <T extends Grantable> T next(Class<T> type, String method) {
        try {
            Method m = type.getDeclaredMethod(method, String.class);
            return next(type, m);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No method " + method + "(String) in class " + type.getName());
        }
    }

    /**
     * Parse the next Grantable in the input string as a custom type.
     *
     * @param type the custom type
     * @param method a valid Method object; the method must be static, it
     * must take a single parameter of type {@link String}, and it must
     * return an instance of the class parameter (or a subclass)
     * @return the next Grantable as a custom type
     * @throws NoSuchElementException if there are no more tokens
     * @throws IllegalArgumentException if the next token is not valid, or
     * if the method is non-static, has an incorrect parameter list or an
     * incorrect return type
     */
    public <T extends Grantable> T next(Class<T> type, Method method) {
        ensureNext();

        RegisteredGrantable dummy = new RegisteredGrantable(method, null);
        String token = tokens.poll();
        Object result = dummy.parse(token);

        return type.cast(result);
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

    /**
     * Register a class as a valid Grantable.
     *
     * @param type the class to register
     */
    public static <T extends Grantable> void register(Class<T> type) {
        if (custom == null) {
            custom = new HashMap<Class<? extends Grantable>, RegisteredGrantable>();
        }
        if (custom.containsKey(type)) {
            return;
        }
        custom.put(type, new RegisteredGrantable(type));
    }

    /**
     * Trim the symbol or prefix part of an input string off, if it exists.
     * <p>
     * If the parser determines that no valid prefix exists in the string,
     * the original string is returned.
     * <p>
     * If the class has not been registered, a temporary registration will be
     * attempted. If the class has no static {@code fromString} method with
     * the {@link GrantableInfo} annotation, an exception will be thrown.
     *
     * @param c the class whose prefix should be trimmed off the input string
     * @param s an input string
     * @return the trimmed string, if a prefix was successfully matched, or
     * the original string
     */
    public static String trim(Class<? extends Grantable> c, String s) {
        RegisteredGrantable reg = core.get(c);
        if (reg == null) {
            reg = custom.get(c);
            if (reg == null) {
                reg = new RegisteredGrantable(c);
            }
        }
        Matcher m = reg.pattern.matcher(s);
        if (m.find() && m.groupCount() >= 2) {
            s = m.group(2);
        }
        return s;
    }

    /**
     * Inner class for keeping track of registered Grantables and their
     * associated patterns and parser methods.
     * <p>
     * Objects scan and extract information from the given class at construct
     * time, which means the constructor throws exceptions.
     */
    private static class RegisteredGrantable {
        private Method method;
        private Pattern pattern;

        public RegisteredGrantable(Method method, Pattern pattern) {
            validateMethod(method);

            this.method  = method;
            this.pattern = (pattern == null) ? Pattern.compile(".*") : pattern;
        }

        /**
         * Create a new RegisteredGrantable from the given type, which must
         * implement the Grantable interface, and must have a valid method
         * with the GrantableInfo annotation.
         *
         * @param type a type that implements Grantable
         */
        public RegisteredGrantable(Class<? extends Grantable> type) {
            for (Method method : type.getDeclaredMethods()) {
                // Grab the annotation
                GrantableInfo info = method.getAnnotation(GrantableInfo.class);
                if (info == null) continue;

                // Validate the method, symbol, and prefix
                validateMethod(method);
                validatePrefix(info);
                validateSymbol(info);

                // Turn prefix into Java regex, prepend symbol if non-null
                String prefix = info.prefix().replaceAll("\\(", "(?:").replaceAll("\\)", ")?");
                if (info.symbol() != '\0') {
                    prefix = "[" + info.symbol() + "]|" + prefix;
                }

                // Surround by parentheses, append the colon and the remainder
                prefix = "(" + prefix + ":)([^,]+)";

                // Store the method, compile the pattern, and finish
                this.method  = method;
                this.pattern = Pattern.compile(prefix);
                return;
            }
            // We only end up down here if no valid methods were found
            throw new IllegalArgumentException("Class " + type.getName() + " has no method annotated with GrantableInfo");
        }

        public boolean matches(String s) {
            return pattern.matcher(s).matches();
        }

        public Grantable parse(String s) {
            Matcher m = pattern.matcher(s);
            if (m.find() && m.groupCount() >= 2) {
                s = m.group(2);
            }
            try {
                return (Grantable) method.invoke(null, s);
            } catch (ReflectiveOperationException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new IllegalArgumentException(e.getCause());
            }
        }

        private static void validateMethod(Method method) {
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Method " + method.getName() + " is non-static");
            }
            if (method.getParameterTypes().length != 1) {
                throw new IllegalArgumentException("Method " + method.getName() + " is has incorrect parameter count");
            }
            if (!String.class.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new IllegalArgumentException("Method " + method.getName() + " has incorrect parameter type");
            }
            if (!method.getDeclaringClass().isAssignableFrom(method.getReturnType())) {
                throw new IllegalArgumentException("Method " + method.getName() + " has incorrect return type");
            }
        }

        private static void validatePrefix(GrantableInfo info) {
            String pre = info.prefix();
            Deque<Integer> open = new ArrayDeque<Integer>();
            for (int i = 0; i < pre.length(); i++) {
                switch (pre.charAt(i)) {
                    case '(':
                        open.push(i);
                        continue;
                    case ')':
                        if (open.isEmpty()) {
                            unmatchedParenthesis(i, "Unmatched close parenthesis:\n" + pre + "\n");
                        }
                        open.pop();
                }
            }
            if (!open.isEmpty()) {
                unmatchedParenthesis(open.peek(), "Unmatched open parenthesis:\n" + pre + "\n");
            }
        }

        private static void unmatchedParenthesis(int i, String msg) {
            for (int j = 0; j < i; j++) {
                msg += ' ';
            }
            msg += '^';
            throw new IllegalArgumentException(msg);
        }

        private static void validateSymbol(GrantableInfo info) {
            if (Character.isAlphabetic(info.symbol()) || Character.isDigit(info.symbol())) {
                throw new IllegalArgumentException("Alpha-numeric symbols are not supported: " + info.symbol());
            }
            switch (info.symbol()) {
                case '\'':
                case '\"':
                case '`':
                case '´':
                    throw new IllegalArgumentException("Quotes and ticks are invalid parser symbols: " + info.symbol());
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                    throw new IllegalArgumentException("Parentheses, brackets, and braces are invalid parser symbols: " + info.symbol());
                case '\\':
                case '/':
                case '|':
                    throw new IllegalArgumentException("Slashes and pipes are invalid parser symbols: " + info.symbol());
            }
        }
    }
}
