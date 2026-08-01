package com.blamejared.controlling.api;

import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.ComboPolicy;
import com.blamejared.controlling.keybinding.ComboKeyBinding;

/**
 * Public API surface for interacting with Controlling combo keybindings.
 *
 * <p>
 * Controlling mixes {@link ComboKeyBinding} into {@link KeyBinding} itself, so every keybinding implements it whenever
 * this class is reachable. These methods therefore cast unconditionally: a {@link ClassCastException} means the mixin
 * did not apply, which is a hard error worth surfacing rather than something to silently fall back from.
 *
 * <p>
 * Internal types appear only inside method bodies, never in a signature, so the api artifact compiles against nothing
 * but Minecraft. The full mod is still required at runtime.
 *
 * <p>
 * Conflict contexts are registered through {@link KeyContextRegistry}.
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
     * Sets the main key and the combo in one call, which the GUI's own reset then picks up. Prefer this over
     * {@link #setComboKeys(KeyBinding, List)} when the main key changes too, so the binding is never briefly half
     * applied.
     */
    public static void setComboKeyBinding(KeyBinding keyBinding, int keyCode, List<Integer> comboKeys) {
        ((ComboKeyBinding) keyBinding).controlling$setComboKeys(comboKeys);
        keyBinding.setKeyCode(keyCode);
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    /**
     * The binding's full display string including its combo, for tooltips and help text: "LCtrl+G" rather than just
     * "G". This is the same string the controls screen shows, so a tooltip and the keybind list cannot disagree.
     *
     * <p>
     * An unbound binding gives whatever vanilla names keycode 0, so test {@link KeyBinding#getKeyCode()} first if that
     * matters.
     *
     * @return the display string, never null.
     */
    public static String getDisplayName(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$getDisplayName();
    }

    /** Sets the conflict context used when detecting keybinding conflicts. */
    public static void setKeyConflictContext(KeyBinding keyBinding, KeyContext keyContext) {
        ((ComboKeyBinding) keyBinding).controlling$setKeyContext(keyContext);
    }

    /** @return the keybinding's conflict context, never null. */
    public static KeyContext getKeyConflictContext(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$getKeyContext();
    }

    /**
     * Controls whether the user may attach combo keys to this binding at all. Use false for modifier-tied binds, or any
     * binding whose meaning would break if extra keys were required.
     *
     * <p>
     * This restricts the controls screen and the visual keyboard only; the owning mod can still set a combo through
     * {@link #setComboKeys(KeyBinding, List)}.
     */
    public static void setAllowsCombos(KeyBinding keyBinding, boolean allows) {
        ((ComboKeyBinding) keyBinding).controlling$setAllowsCombos(allows);
    }

    /** @return whether the user may build a combo on this binding. */
    public static boolean allowsCombos(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$allowsCombos();
    }

    /**
     * Bars specific keys from this binding's combo, leaving every other key usable. Use for a key the mod reads itself,
     * such as a modifier that already means something for this binding.
     *
     * <p>
     * Restricts the GUI only, as {@link #setAllowsCombos(KeyBinding, boolean)} does. Passing no keys clears the list.
     */
    public static void setBlockedComboKeys(KeyBinding keyBinding, int... keys) {
        ((ComboKeyBinding) keyBinding).controlling$setBlockedComboKeys(keys);
    }

    /** @return true when {@code keyCode} may not be placed in this binding's combo by the user. */
    public static boolean isComboKeyBlocked(KeyBinding keyBinding, int keyCode) {
        final ComboKeyBinding comboKeyBinding = (ComboKeyBinding) keyBinding;
        return !ComboPolicy.accepts(
                keyCode,
                comboKeyBinding.controlling$allowsCombos(),
                comboKeyBinding.controlling$blockedComboKeys());
    }

    /** Controls whether the main key of this binding may be set to a mouse button. Use false to forbid mouse binds. */
    public static void setAllowsMouse(KeyBinding keyBinding, boolean allows) {
        ((ComboKeyBinding) keyBinding).controlling$setAllowsMouse(allows);
    }

    /** @return whether the main key may be bound to a mouse button. */
    public static boolean allowsMouse(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$allowsMouse();
    }

    /**
     * Controls whether the main key of this binding may be set to a keyboard key. Use false to forbid keyboard binds.
     */
    public static void setAllowsKeyboard(KeyBinding keyBinding, boolean allows) {
        ((ComboKeyBinding) keyBinding).controlling$setAllowsKeyboard(allows);
    }

    /** @return whether the main key may be bound to a keyboard key. */
    public static boolean allowsKeyboard(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$allowsKeyboard();
    }

    /** @return the extra combo keycodes for this bind, empty when it is a plain single-key bind. */
    public static List<Integer> getComboKeys(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$getComboKeys();
    }

    /** Replaces the extra combo keycodes for this bind. */
    public static void setComboKeys(KeyBinding keyBinding, List<Integer> keys) {
        ((ComboKeyBinding) keyBinding).controlling$setComboKeys(keys);
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    /**
     * Sets the combo this binding resets to. A binding still sitting on its old default is moved to the new one, so
     * changing a shipped default reaches players who never customised it while leaving customised bindings alone.
     */
    public static void setDefaultComboKeys(KeyBinding keyBinding, List<Integer> keys) {
        final ComboKeyBinding comboKeyBinding = (ComboKeyBinding) keyBinding;
        final boolean wasDefault = comboKeyBinding.controlling$isSetToDefaultValue();
        comboKeyBinding.controlling$setDefaultComboKeys(keys);
        if (wasDefault) {
            comboKeyBinding.controlling$setComboKeys(keys);
            KeyBinding.resetKeyBindingArrayAndHash();
        }
    }

    /**
     * Live poll of the current keyboard and mouse state: true when the binding's main key and every combo key are down
     * right now. Safe to call from anywhere on the client - in game, inside any GUI, and between ticks - because it
     * reads input state directly rather than vanilla's press flags.
     *
     * <p>
     * Ignores conflict context and more specific bindings; use {@link #isComboActive(KeyBinding)} to ask whether the
     * binding would actually fire.
     */
    public static boolean isComboDown(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$isComboDown();
    }

    /**
     * Live poll as {@link #isComboDown(KeyBinding)}, additionally requiring the binding's {@link KeyContext} to be
     * active and no more specific binding to be held. With both {@code G} and {@code Ctrl+G} bound, holding Ctrl+G
     * reports only {@code Ctrl+G} as active.
     */
    public static boolean isComboActive(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$isComboActive();
    }

    /** @return ticks the combo has been held (0 = first tick), or -1 when not held. */
    public static int getComboHeldTicks(KeyBinding keyBinding) {
        return ((ComboKeyBinding) keyBinding).controlling$getComboHeldTicks();
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
