package com.blamejared.controlling.keybinding;

import java.util.function.IntPredicate;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.blamejared.controlling.api.ControllingApi;

/**
 * Reads live key/mouse state per the mouse-keycode convention. Modifier keycodes are treated left/right agnostically so
 * a combo storing LCONTROL is satisfied by either control key, matching the legacy modifier behavior.
 */
public final class InputState {

    /** Reusable predicate; stateless so a single instance avoids per-tick allocation. */
    public static final IntPredicate IS_DOWN = InputState::isDown;

    private InputState() {}

    public static boolean isDown(int keyCode) {
        if (keyCode == ComboState.KEY_NONE) {
            return false;
        }
        if (ControllingApi.isMouseKeyCode(keyCode)) {
            final int button = keyCode - ControllingApi.MOUSE_KEYCODE_OFFSET;
            return button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
        }
        final KeyModifier modifier = KeyModifier.fromKeyCode(keyCode);
        if (modifier != KeyModifier.NONE) {
            return modifier.isActive(); // either left or right side counts
        }
        return keyCode > 0 && keyCode < Keyboard.getKeyCount() && Keyboard.isKeyDown(keyCode);
    }
}
