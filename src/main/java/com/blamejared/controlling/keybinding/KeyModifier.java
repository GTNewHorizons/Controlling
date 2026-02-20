package com.blamejared.controlling.keybinding;

import org.lwjgl.input.Keyboard;

public enum KeyModifier {

    NONE("None", -1, -1),
    CONTROL("Ctrl", Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL),
    SHIFT("Shift", Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT),
    ALT("Alt", Keyboard.KEY_LMENU, Keyboard.KEY_RMENU);

    private final String displayName;
    private final int leftKeyCode;
    private final int rightKeyCode;

    KeyModifier( String displayName, int leftKeyCode, int rightKeyCode) {
        this.displayName = displayName;
        this.leftKeyCode = leftKeyCode;
        this.rightKeyCode = rightKeyCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this != NONE && (Keyboard.isKeyDown(leftKeyCode) || Keyboard.isKeyDown(rightKeyCode));
    }

    public boolean matches(int keyCode) {
        return this != NONE && (keyCode == leftKeyCode || keyCode == rightKeyCode);
    }

    public static KeyModifier fromSerializedName(String name) {
        for (KeyModifier value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return NONE;
    }

    public static KeyModifier fromKeyCode(int keyCode) {
        for (KeyModifier value : values()) {
            if (value.matches(keyCode)) {
                return value;
            }
        }
        return NONE;
    }

    public static KeyModifier getActiveModifier() {
        if (CONTROL.isActive()) {
            return CONTROL;
        }
        if (SHIFT.isActive()) {
            return SHIFT;
        }
        if (ALT.isActive()) {
            return ALT;
        }
        return NONE;
    }

    public static boolean isAnyModifierActive() {
        return CONTROL.isActive() || SHIFT.isActive() || ALT.isActive();
    }

    public static boolean isKeyCodeModifier(int keyCode) {
        return fromKeyCode(keyCode) != NONE;
    }
}
