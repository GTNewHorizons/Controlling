package com.blamejared.controlling.api;

/**
 * Public API representation of a key combo modifier.
 *
 * <p>
 * Holds no internal type in any field or method signature, so the api artifact resolves on its own; the mapping to
 * {@code KeyModifier} lives in {@link ControllingApi}.
 */
public enum ComboModifier {

    NONE,
    CONTROL,
    SHIFT,
    ALT
}
