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
     * Whether binds in this context may fire right now. Consulted every client tick by the combo poller, so keep it
     * cheap. Defaulted to always-active for implementors that only care about conflicts.
     */
    default boolean isActive() {
        return true;
    }

    /**
     * Lang key for this context's display name, shown in the controls screen. Defaults to
     * {@code "options.context." + id()}, so a custom context only needs to add that key to its lang files.
     *
     * @return the translation key for this context's display name.
     */
    default String translationKey() {
        return "options.context." + id();
    }
}
