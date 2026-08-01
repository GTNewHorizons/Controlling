package com.blamejared.controlling.api;

import java.util.Locale;

import net.minecraft.client.Minecraft;

/** Built-in {@link KeyContext} values. */
public enum KeyContexts implements KeyContext {

    /** Active/relevant everywhere; conflicts with every context. */
    UNIVERSAL,
    /** World-only binds; conflicts only with other IN_GAME binds. */
    IN_GAME,
    /** GUI-only binds; conflicts only with other GUI binds. */
    GUI;

    /** Precomputed: id() is called per row per frame by the controls list, so it must not build a string. */
    private final String id = name().toLowerCase(Locale.ROOT);
    private final String translationKey = "options.context." + this.id;

    @Override
    public String id() {
        return this.id;
    }

    /** Lang key for this context's display name. */
    public String translationKey() {
        return this.translationKey;
    }

    @Override
    public boolean conflicts(KeyContext other) {
        return switch (this) {
            case IN_GAME -> other == IN_GAME;
            case GUI -> other == GUI;
            default -> true;
        };
    }

    @Override
    public boolean isActive() {
        return switch (this) {
            case IN_GAME -> Minecraft.getMinecraft().currentScreen == null;
            case GUI -> Minecraft.getMinecraft().currentScreen != null;
            default -> true;
        };
    }
}
