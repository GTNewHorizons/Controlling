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
     * Sets the default combo modifier for a keybinding. The default key code still comes from
     * {@link KeyBinding#getKeyCodeDefault()}.
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

    /**
     * Prevents combo modifiers from being set on a keybinding. When disabled, the current and default modifiers are
     * reset to {@link ComboModifier#NONE} and later modifier changes (GUI, options file, API) are ignored.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setModifiersDisabled(KeyBinding keyBinding, boolean disabled) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setModifiersDisabled(disabled);
        KeyBinding.resetKeyBindingArrayAndHash();
        return true;
    }

    /**
     * @return true when combo modifiers are disabled for the keybinding.
     */
    public static boolean areModifiersDisabled(KeyBinding keyBinding) {
        return keyBinding instanceof ComboKeyBinding comboKeyBinding
                && comboKeyBinding.controlling$areModifiersDisabled();
    }
}
