package com.blamejared.controlling.api;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.KeyModifier;

/**
 * Public API surface for interacting with Controlling combo keybindings.
 */
public final class ControllingApi {

    private ControllingApi() {}

    /**
     * @return true when the keybinding supports combo modifiers.
     */
    public static boolean supportsComboKeyBinding(KeyBinding keyBinding) {
        return keyBinding instanceof ComboKeyBinding;
    }

    /**
     * @return the active combo modifier, or {@link ComboModifier#NONE} when combos are unavailable.
     */
    public static ComboModifier getComboModifier(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return ComboModifier.fromInternal(comboKeyBinding.controlling$getKeyModifier());
        }
        return ComboModifier.NONE;
    }

    /**
     * @return the default combo modifier, or {@link ComboModifier#NONE} when combos are unavailable.
     */
    public static ComboModifier getDefaultComboModifier(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return ComboModifier.fromInternal(comboKeyBinding.controlling$getDefaultKeyModifier());
        }
        return ComboModifier.NONE;
    }

    /**
     * Sets the current combo binding value.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setComboKeyBinding(KeyBinding keyBinding, ComboModifier comboModifier, int keyCode) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        final KeyModifier keyModifier = comboModifier == null ? KeyModifier.NONE : comboModifier.toInternal();
        comboKeyBinding.controlling$setKeyModifierAndCode(keyModifier, keyCode);
        KeyBinding.resetKeyBindingArrayAndHash();
        return true;
    }

    /**
     * Sets the default combo modifier for a keybinding.
     * The default key code still comes from {@link KeyBinding#getKeyCodeDefault()}.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setDefaultComboKeyBinding(KeyBinding keyBinding, ComboModifier comboModifier) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        final boolean wasDefault = comboKeyBinding.controlling$isSetToDefaultValue();
        final KeyModifier keyModifier = comboModifier == null ? KeyModifier.NONE : comboModifier.toInternal();
        comboKeyBinding.controlling$setDefaultKeyModifier(keyModifier);
        if (wasDefault) {
            comboKeyBinding.controlling$setKeyModifier(keyModifier);
        }
        return true;
    }
}
