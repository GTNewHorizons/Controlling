package com.blamejared.controlling.api;

import java.util.Locale;

/** Built-in {@link KeyContext} values. */
public enum KeyContexts implements KeyContext {

    /** Active/relevant everywhere; conflicts with every context. */
    UNIVERSAL,
    /** World-only binds; conflicts only with other IN_GAME binds. */
    IN_GAME,
    /** GUI-only binds; conflicts only with other GUI binds. */
    GUI;

    @Override
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean conflicts(KeyContext other) {
        switch (this) {
            case UNIVERSAL:
                return true;
            case IN_GAME:
                return other == IN_GAME;
            case GUI:
                return other == GUI;
            default:
                return true;
        }
    }
}
