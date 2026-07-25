package com.blamejared.controlling.api;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.KeyModifier;

/**
 * Public API surface for interacting with Controlling combo keybindings.
 */
public final class ControllingApi {

    private ControllingApi() {}

    /** Keycode offset used to encode mouse buttons: mouse button b becomes keycode b + MOUSE_KEYCODE_OFFSET. */
    public static final int MOUSE_KEYCODE_OFFSET = -100;

    /** @return the keycode encoding for a mouse button index (0 = LMB -> -100). */
    public static int mouseButtonToKeyCode(int button) {
        return button + MOUSE_KEYCODE_OFFSET;
    }

    /** @return true when the keycode encodes a mouse button (LMB/RMB/MMB/...). */
    public static boolean isMouseKeyCode(int keyCode) {
        return keyCode <= MOUSE_KEYCODE_OFFSET;
    }

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
        KeyModifier keyModifier = comboModifier == null ? KeyModifier.NONE : comboModifier.toInternal();
        if (!comboKeyBinding.controlling$allowsComboModifier()) {
            keyModifier = KeyModifier.NONE;
        }
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
     * Sets the conflict context used when detecting keybinding conflicts.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setKeyConflictContext(KeyBinding keyBinding, KeyContext keyContext) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setKeyContext(keyContext);
        return true;
    }

    /**
     * @return the keybinding's conflict context, or {@link KeyContexts#UNIVERSAL} when combos are unavailable.
     */
    public static KeyContext getKeyConflictContext(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$getKeyContext();
        }
        return KeyContexts.UNIVERSAL;
    }

    /** Registers a custom conflict context (built-in ids are reserved). */
    public static void registerKeyContext(KeyContext keyContext) {
        KeyContextRegistry.register(keyContext);
    }

    /** @return the registered context for {@code id}, or {@code null} if unknown. */
    public static KeyContext getKeyContext(String id) {
        return KeyContextRegistry.get(id);
    }

    /**
     * Controls whether a user may attach a combo modifier (Ctrl/Shift/Alt) to this keybinding. Use false for
     * modifier-tied binds.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setAllowsComboModifier(KeyBinding keyBinding, boolean allows) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setAllowsComboModifier(allows);
        return true;
    }

    /**
     * @return whether a combo modifier may be attached; true when the keybinding does not support combos (plain vanilla
     *         binding).
     */
    public static boolean allowsComboModifier(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$allowsComboModifier();
        }
        return true;
    }

    /**
     * Controls whether the main key of this binding may be set to a mouse button. Use false to forbid mouse binds.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setAllowsMouse(KeyBinding keyBinding, boolean allows) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setAllowsMouse(allows);
        return true;
    }

    /**
     * @return whether the main key may be bound to a mouse button; true when the keybinding does not support combos.
     */
    public static boolean allowsMouse(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$allowsMouse();
        }
        return true;
    }

    /**
     * Controls whether the main key of this binding may be set to a keyboard key. Use false to forbid keyboard binds.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setAllowsKeyboard(KeyBinding keyBinding, boolean allows) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setAllowsKeyboard(allows);
        return true;
    }

    /**
     * @return whether the main key may be bound to a keyboard key; true when the keybinding does not support combos.
     */
    public static boolean allowsKeyboard(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$allowsKeyboard();
        }
        return true;
    }

    /** @return the extra chord keycodes for this bind, or an empty list when combos are unavailable. */
    public static List<Integer> getComboKeys(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$getComboKeys();
        }
        return Collections.emptyList();
    }

    /** @return false when the keybinding does not support combos. */
    public static boolean setComboKeys(KeyBinding keyBinding, List<Integer> keys) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setComboKeys(keys);
        KeyBinding.resetKeyBindingArrayAndHash();
        return true;
    }

    /** @return false when the keybinding does not support combos. */
    public static boolean setDefaultComboKeys(KeyBinding keyBinding, List<Integer> keys) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setDefaultComboKeys(keys);
        return true;
    }

    /**
     * Live poll of the current keyboard and mouse state: true when the binding's main key and every combo key are down
     * right now. Safe to call from anywhere on the client - in game, inside any GUI, and between ticks - because it
     * reads input state directly rather than vanilla's press flags.
     *
     * <p>
     * Ignores conflict context and more specific bindings; use {@link #isChordActive(KeyBinding)} to ask whether the
     * binding would actually fire.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean isChordDown(KeyBinding keyBinding) {
        return keyBinding instanceof ComboKeyBinding comboKeyBinding && comboKeyBinding.controlling$isChordDown();
    }

    /**
     * Live poll as {@link #isChordDown(KeyBinding)}, additionally requiring the binding's {@link KeyContext} to be
     * active and no more specific binding to be held. With both {@code G} and {@code Ctrl+G} bound, holding Ctrl+G
     * reports only {@code Ctrl+G} as active.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean isChordActive(KeyBinding keyBinding) {
        return keyBinding instanceof ComboKeyBinding comboKeyBinding && comboKeyBinding.controlling$isChordActive();
    }

    /** @return ticks the combo has been held (0 = first tick), or -1 when not held / unsupported. */
    public static int getComboHeldTicks(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$getComboHeldTicks();
        }
        return -1;
    }

    /** @return true while the combo is satisfied this tick. */
    public static boolean isComboPressed(KeyBinding keyBinding) {
        return getComboHeldTicks(keyBinding) >= 0;
    }

    /** @return true only on the first tick the combo became satisfied. */
    public static boolean isComboFirstPressed(KeyBinding keyBinding) {
        return getComboHeldTicks(keyBinding) == 0;
    }

    /** @return true on the first press or once it has been held at least minHeldTicks (key-repeat style). */
    public static boolean isComboPressedOrHeld(KeyBinding keyBinding, int minHeldTicks) {
        final int held = getComboHeldTicks(keyBinding);
        return held == 0 || (held >= minHeldTicks && minHeldTicks >= 0);
    }
}
