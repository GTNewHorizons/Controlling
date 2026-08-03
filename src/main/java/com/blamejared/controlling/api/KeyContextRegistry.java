package com.blamejared.controlling.api;

import java.util.HashMap;
import java.util.Map;

/** Registry of {@link KeyContext} by id. Built-ins are pre-registered and reserved. */
public final class KeyContextRegistry {

    private static final Map<String, KeyContext> CONTEXTS = new HashMap<>();

    static {
        for (KeyContexts context : KeyContexts.values()) {
            CONTEXTS.put(context.id(), context);
        }
    }

    private KeyContextRegistry() {}

    /**
     * Registers a custom context. A custom ID overwrites a prior custom entry; the three built-in IDs
     * ({@code universal}, {@code in_game}, {@code gui}) are reserved and such registrations are ignored.
     */
    public static void register(KeyContext context) {
        if (context == null) {
            return;
        }
        final String id = context.id();
        if (id == null) {
            return;
        }
        final KeyContext existing = CONTEXTS.get(id);
        if (existing instanceof KeyContexts) {
            return; // reserved built-in id
        }
        CONTEXTS.put(id, context);
    }

    /** @return the context registered under {@code id}, or {@code null} if unknown. */
    public static KeyContext get(String id) {
        return CONTEXTS.get(id);
    }
}
