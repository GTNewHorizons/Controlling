package com.blamejared.controlling.api;

/**
 * A keybinding conflict context, modelled on Forge's IKeyConflictContext. Callers compute overall clash as
 * {@code a.conflicts(b) || b.conflicts(a)}, so {@link #conflicts(KeyContext)} may be asymmetric.
 * <p>
 * Instances are singletons compared by identity and registered once with {@link KeyContextRegistry}; see
 * {@link KeyContexts} for the built-ins.
 */
public abstract class KeyContext {

    private final String id;
    private final String translationKey;

    /** Uses {@code "options.context." + id} as the translation key. */
    protected KeyContext(String id) {
        this(id, "options.context." + id);
    }

    /**
     * @param id             a stable, unique identifier, e.g. "universal", "in_game", "gui", "ae2".
     * @param translationKey the lang key for this context's display name.
     */
    protected KeyContext(String id, String translationKey) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("key context id must not be null or empty");
        }
        if (translationKey == null || translationKey.isEmpty()) {
            throw new IllegalArgumentException("key context translation key must not be null or empty");
        }
        this.id = id;
        this.translationKey = translationKey;
    }

    /** @return this context's identifier. */
    public final String id() {
        return this.id;
    }

    /** @return the lang key for this context's display name. */
    public final String translationKey() {
        return this.translationKey;
    }

    /**
     * Whether a binding in this context clashes with one in {@code other}, i.e. both could fire from the same key
     * press. {@code true} means the two contexts cannot share a key; {@code false} means they can. For example,
     * {@code IN_GAME.conflicts(GUI)} is {@code false}, since only one of the two is ever active, so the same key can
     * serve both.
     *
     * @param other the other binding's context, never {@code null}.
     * @return {@code true} when the two contexts clash.
     */
    public abstract boolean conflicts(KeyContext other);

    /**
     * Whether binds in this context may fire right now. Polled every client tick, so keep it cheap.
     *
     * @return {@code true} if currently active. Defaults to always active.
     */
    public boolean isActive() {
        return true;
    }
}
