package com.garbagemule.MobArena.grantable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link GrantableParser} requires that classes registered for generic
 * parsing have exactly one method annotated with GrantableInfo.
 * <p>
 * The annotation is required to provide a prefix in the form of a type of
 * regular expression that allows the parser to distinguish the annotated
 * Grantable from other Grantables. The expression should be unique, such
 * that it does not override the core patterns, even though this is allowed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GrantableInfo {
    /**
     * A short-hand symbol that appears before a parsable string.
     * <p>
     * Use this ONLY if you are absolutely sure that you know what you are
     * doing! Using symbols like single letters, numbers, apostrophes, or
     * other special symbols like tics {@code `´} and slashes {@code /\}
     * can have dire consequences for the parsability of the config-files!
     *
     * @return a short-hand symbol for parsing (default is the null char,
     * which is ignored by the parser)
     */
    public char symbol() default '\0';

    /**
     * A pseudo-regular expression of the valid prefix(es) for this Grantable
     * that appear before the colon {@code :} in the config-file. The format
     * supports alphanumeric characters, and optional parts can be enclosed in
     * parentheses.
     * <p>
     * An example is the permission prefix, which is {@code p(erm(ission))},
     * which matches both {@code p}, {@code perm}, and {@code permission}.
     *
     * @return a pseudo-regular expression of the valid prefix(es) for this
     * Grantable
     */
    public String prefix();
}
