package com.blamejared.controlling.api;

/**
 * A keybinding conflict context, modelled on Forge's IKeyConflictContext. Two bindings on the same key only conflict
 * when their contexts clash (see {@link com.blamejared.controlling.api.ControllingApi} conflict handling). Overall
 * clash is computed by callers as {@code a.conflicts(b) || b.conflicts(a)}, so {@code conflicts} may be asymmetric.
 */
public interface KeyContext {

    /** Stable identifier, e.g. "universal", "in_game", "gui", "ae2". */
    String id();

    /** Whether a binding in this context clashes with one in {@code other}. */
    boolean conflicts(KeyContext other);

    /**
     * Reserved for a future activation-gating phase; NOT consulted yet. Defaulted so adding usage later does not break
     * implementors.
     */
    default boolean isActive() {
        return true;
    }
}
