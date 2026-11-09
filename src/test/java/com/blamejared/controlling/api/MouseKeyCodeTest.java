package com.blamejared.controlling.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MouseKeyCodeTest {

    @Test
    void everyMouseButtonRoundTrips() {
        for (int button = 0; button < 8; button++) {
            final int keyCode = ControllingApi.mouseButtonToKeyCode(button);
            assertTrue(ControllingApi.isMouseKeyCode(keyCode), "button " + button + " -> " + keyCode);
            assertEquals(button, keyCode - ControllingApi.MOUSE_KEYCODE_OFFSET);
        }
    }

    @Test
    void rightAndMiddleAreMouseCodes() {
        // regression: these encode above the offset, so a "<= offset" test missed them
        assertTrue(ControllingApi.isMouseKeyCode(-99));
        assertTrue(ControllingApi.isMouseKeyCode(-98));
    }

    @Test
    void keyboardCodesAreNotMouse() {
        assertFalse(ControllingApi.isMouseKeyCode(0)); // KEY_NONE
        assertFalse(ControllingApi.isMouseKeyCode(29)); // LCONTROL
        assertFalse(ControllingApi.isMouseKeyCode(57)); // SPACE
    }
}
