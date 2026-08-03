package com.blamejared.controlling.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of {@link KeyContext} by ID. Built-ins are pre-registered and reserved.
 * <p>
 * Not synchronized: register from the client thread during mod construction or client init, before any keybinding is
 * polled. Reads after that point are safe because the map is never mutated again.
 */
public final class KeyContextRegistry {

    private static final Map<String, KeyContext> CONTEXTS = new HashMap<>();

    static {
        for (KeyContexts context : KeyContexts.values()) {
            CONTEXTS.put(context.id(), context);
        }
    }

    private KeyContextRegistry() {}

    /**
     * Registers a custom context. See the class docs for when and from which thread this may be called.
     *
     * @param context the context to register.
     * @throws IllegalArgumentException when {@code context} or its ID is {@code null}, when the ID is one of the
     *                                  reserved built-ins ({@code universal}, {@code in_game}, {@code gui}), or when
     *                                  another context is already registered under that ID. Registration is not
     *                                  silently dropped, so a load-order clash between two mods is visible instead of
     *                                  picking an arbitrary winner.
     */
    public static void register(KeyContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        final String id = context.id();
        if (id == null) {
            throw new IllegalArgumentException("context ID must not be null: " + context.getClass().getName());
        }
        final KeyContext existing = CONTEXTS.get(id);
        if (existing instanceof KeyContexts) {
            throw new IllegalArgumentException("key context ID '" + id + "' is reserved for a built-in context");
        }
        if (existing != null) {
            throw new IllegalArgumentException(
                    "key context ID '" + id + "' is already registered by " + existing.getClass().getName());
        }
        CONTEXTS.put(id, context);
    }

    /**
     * @param id the ID to look up.
     * @return the context registered under {@code id}, or {@code null} if unknown.
     */
    public static KeyContext get(String id) {
        return CONTEXTS.get(id);
    }
}
