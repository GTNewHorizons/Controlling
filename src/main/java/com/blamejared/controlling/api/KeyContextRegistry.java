package com.blamejared.controlling.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Registry of {@link KeyContext} by ID. Built-ins are pre-registered and their ids are reserved.
 * <p>
 * Not synchronized: register from the client thread during mod construction or client init, before any keybinding is
 * polled. Reads after that point are safe because the map is never mutated again.
 */
public final class KeyContextRegistry {

    private static final Map<String, KeyContext> CONTEXTS = new HashMap<>();
    private static final Set<String> RESERVED = new HashSet<>();

    static {
        for (KeyContext context : KeyContexts.VALUES) {
            CONTEXTS.put(context.id(), context);
            RESERVED.add(context.id());
        }
    }

    private KeyContextRegistry() {}

    /**
     * Registers a custom context. See the class docs for when and from which thread this may be called.
     *
     * @param context the context to register.
     * @throws IllegalArgumentException when the ID is one of the reserved built-ins ({@code universal},
     *                                  {@code in_game}, {@code gui}), or when another context is already registered
     *                                  under that ID. Registration is not silently dropped, so a load-order clash
     *                                  between two mods is visible instead of picking an arbitrary winner.
     * @throws NullPointerException     when {@code context} is {@code null}.
     */
    public static void register(KeyContext context) {
        Objects.requireNonNull(context, "context must not be null");
        final String id = context.id();
        if (RESERVED.contains(id)) {
            throw new IllegalArgumentException("key context id '" + id + "' is reserved for a built-in context");
        }
        final KeyContext existing = CONTEXTS.get(id);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "key context id '" + id + "' is already registered by " + existing.getClass().getName());
        }
        CONTEXTS.put(id, context);
    }

    /**
     * Look up a `KeyContext` by its unique ID.
     *
     * @param id the ID to look up.
     * @return the context registered under {@code id}, or {@code null} if unknown.
     */
    public static KeyContext get(String id) {
        return CONTEXTS.get(id);
    }
}
