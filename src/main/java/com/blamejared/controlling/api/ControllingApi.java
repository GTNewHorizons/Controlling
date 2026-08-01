package com.blamejared.controlling.api;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.ComboKeyBinding;

/**
 * Public API surface for interacting with Controlling combo keybindings.
 *
 * <p>
 * Internal types appear only inside method bodies, never in a signature, so the api artifact compiles against nothing
 * but Minecraft. The full mod is still required at runtime.
 */
public final class ControllingApi {

    private ControllingApi() {}

    /** Keycode offset used to encode mouse buttons: mouse button b becomes keycode b + MOUSE_KEYCODE_OFFSET. */
    public static final int MOUSE_KEYCODE_OFFSET = -100;

    /** @return the keycode encoding for a mouse button index (0 = LMB -> -100). */
    public static int mouseButtonToKeyCode(int button) {
        return button + MOUSE_KEYCODE_OFFSET;
    }

    /**
     * Mouse buttons encode upward from the offset (LMB -100, RMB -99, MMB -98, ...), so every mouse keycode is negative
     * and no keyboard keycode is. Matches vanilla, which treats any negative keycode as a mouse button.
     *
     * @return true when the keycode encodes a mouse button (LMB/RMB/MMB/...).
     */
    public static boolean isMouseKeyCode(int keyCode) {
        return keyCode < 0;
    }

    /**
     * @return true when the keybinding supports combo modifiers.
     */
    public static boolean supportsComboKeyBinding(KeyBinding keyBinding) {
        return keyBinding instanceof ComboKeyBinding;
    }

    /**
     * Sets the main key and the chord in one call, which the GUI's own reset then picks up. Prefer this over
     * {@link #setComboKeys(KeyBinding, List)} when the main key changes too, so the binding is never briefly half
     * applied.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setComboKeyBinding(KeyBinding keyBinding, int keyCode, List<Integer> chordKeys) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        comboKeyBinding.controlling$setComboKeys(chordKeys);
        keyBinding.setKeyCode(keyCode);
        KeyBinding.resetKeyBindingArrayAndHash();
        return true;
    }

    /**
     * The binding's full display string including its chord, for tooltips and help text: "LCtrl+G" rather than just
     * "G". This is the same string the controls screen shows, so a tooltip and the keybind list cannot disagree.
     *
     * <p>
     * Safe for any keybinding: one without combo support falls back to the vanilla key name. An unbound binding gives
     * whatever vanilla names keycode 0, so test {@link KeyBinding#getKeyCode()} first if that matters.
     *
     * @return the display string, never null.
     */
    public static String getDisplayName(KeyBinding keyBinding) {
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            return comboKeyBinding.controlling$getDisplayName();
        }
        return GameSettings.getKeyDisplayString(keyBinding.getKeyCode());
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

    /**
     * Sets the chord this binding resets to. A binding still sitting on its old default is moved to the new one, so
     * changing a shipped default reaches players who never customised it while leaving customised bindings alone.
     *
     * @return false when the keybinding does not support combos.
     */
    public static boolean setDefaultComboKeys(KeyBinding keyBinding, List<Integer> keys) {
        if (!(keyBinding instanceof ComboKeyBinding comboKeyBinding)) {
            return false;
        }
        final boolean wasDefault = comboKeyBinding.controlling$isSetToDefaultValue();
        comboKeyBinding.controlling$setDefaultComboKeys(keys);
        if (wasDefault) {
            comboKeyBinding.controlling$setComboKeys(keys);
            KeyBinding.resetKeyBindingArrayAndHash();
        }
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
