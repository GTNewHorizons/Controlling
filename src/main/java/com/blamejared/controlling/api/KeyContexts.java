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

    @Override
    public boolean isActive() {
        switch (this) {
            case IN_GAME:
                return Minecraft.getMinecraft().currentScreen == null;
            case GUI:
                return Minecraft.getMinecraft().currentScreen != null;
            case UNIVERSAL:
            default:
                return true;
        }
    }
}
