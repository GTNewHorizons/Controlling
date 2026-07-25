package com.blamejared.controlling.keybinding;

import java.util.function.IntPredicate;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.blamejared.controlling.api.ControllingApi;

/**
 * Reads live key/mouse state per the mouse-keycode convention. Keys match by exact physical code, so left and right
 * modifier keys (LSHIFT vs RSHIFT, etc.) are distinct: a combo storing RSHIFT is satisfied only by the right shift.
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
        return keyCode > 0 && keyCode < Keyboard.getKeyCount() && Keyboard.isKeyDown(keyCode);
    }
}
