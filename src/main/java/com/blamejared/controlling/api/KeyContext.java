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

    /**
     * Uses {@code "options.context." + id} as the translation key.
     *
     * @param id a stable, unique identifier, e.g. "universal", "in_game", "gui", "ae2".
     */
    protected KeyContext(String id) {
        this(id, "options.context." + id);
    }

    /**
     * @param id             a stable, unique identifier, e.g. "universal", "in_game", "gui", "ae2".
     * @param translationKey the lang key for this context's display name.
     * @throws IllegalArgumentException when {@code id} or {@code translationKey} is {@code null} or empty.
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
     * Whether binds in this context may fire right now. Polled every client tick, so keep it cheap.
     * <p>
     * Abstract because it is the one fact only the implementor knows. Everything else about a context follows from it:
     * a context that is always active overlaps every other live context, and so clashes with all of them.
     *
     * @return {@code true} if currently active.
     */
    public abstract boolean isActive();

    /**
     * Whether a binding in this context clashes with one in {@code other}, i.e. both could fire from the same key
     * press. {@code true} means the two contexts cannot share a key; {@code false} means they can.
     * <p>
     * Defaults to {@code true}, which is always safe: a missed clash is worse than a spurious warning. Override only to
     * narrow it, and only when {@link #isActive()} makes the two windows genuinely disjoint. An always-active context
     * must not narrow this, since it overlaps everything by definition.
     * <p>
     * Examples, using the built-ins:
     * <ul>
     * <li>{@code UNIVERSAL.conflicts(GUI)} is {@code true} - a universal bind is live inside GUIs too.</li>
     * <li>{@code IN_GAME.conflicts(GUI)} is {@code false} - one is only live with a screen open, the other only with no
     * screen open, so the same key can serve both.</li>
     * <li>{@code IN_GAME.conflicts(IN_GAME)} is {@code true} - both are live at the same time.</li>
     * </ul>
     * <p>
     * Returning {@code false} for an unrecognized context is safe: callers evaluate both directions and
     * {@link KeyContexts#UNIVERSAL} returns {@code true} from its own side.
     *
     * @param other the other binding's context, never {@code null}.
     * @return {@code true} when the two contexts clash.
     */
    public boolean conflicts(KeyContext other) {
        return true;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
