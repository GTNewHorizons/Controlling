package com.blamejared.controlling.api;

import com.blamejared.controlling.keybinding.KeyModifier;

/**
 * Public API representation of a key combo modifier.
 */
public enum ComboModifier {

    NONE(KeyModifier.NONE),
    CONTROL(KeyModifier.CONTROL),
    SHIFT(KeyModifier.SHIFT),
    ALT(KeyModifier.ALT);

    private final KeyModifier internal;

    ComboModifier(KeyModifier internal) {
        this.internal = internal;
    }

    /** Package-private: KeyModifier is internal, so it must not cross the api package boundary. */
    KeyModifier toInternal() {
        return this.internal;
    }

    /** Package-private for the same reason as {@link #toInternal()}. */
    static ComboModifier fromInternal(KeyModifier internal) {
        if (internal == null) {
            return NONE;
        }
        return switch (internal) {
            case CONTROL -> CONTROL;
            case SHIFT -> SHIFT;
            case ALT -> ALT;
            case NONE -> NONE;
        };
    }
}
