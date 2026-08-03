package com.blamejared.controlling.api;

/**
 * A keybinding conflict context, modelled on Forge's IKeyConflictContext. Two bindings on the same key only conflict
 * when their contexts clash. Overall clash is computed by callers as {@code a.conflicts(b) || b.conflicts(a)}, so
 * {@link #conflicts(KeyContext)} may be asymmetric.
 * <p>
 * Implementations are singletons registered once with {@link KeyContextRegistry}; see {@link KeyContexts} for the
 * built-ins.
 */
public interface KeyContext {

    /**
     * @return a stable identifier, e.g. "universal", "in_game", "gui", "ae2". Must not change over the lifetime of the
     *         instance, and must be unique across all registered contexts.
     */
    String id();

    /**
     * Whether a binding in this context clashes with one in {@code other}, i.e. whether both could fire from the same
     * key press. Returning {@code true} means the two contexts cannot share a key; {@code false} means they can.
     * <p>
     * Examples, using the built-ins:
     * <ul>
     * <li>{@code UNIVERSAL.conflicts(GUI)} is {@code true} - a universal bind is live inside GUIs too.</li>
     * <li>{@code IN_GAME.conflicts(GUI)} is {@code false} - one is only live with a screen open, the other only with no
     * screen open, so the same key can serve both.</li>
     * <li>{@code IN_GAME.conflicts(IN_GAME)} is {@code true} - both are live at the same time.</li>
     * </ul>
     *
     * @param other the context of the other binding, never {@code null}.
     * @return {@code true} when the two contexts clash.
     */
    boolean conflicts(KeyContext other);

    /**
     * Whether binds in this context may fire right now. Consulted every client tick by the combo poller, so keep it
     * cheap. Defaulted to always-active for implementors that only care about conflicts.
     *
     * @return {@code true} when binds in this context are currently allowed to fire.
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
