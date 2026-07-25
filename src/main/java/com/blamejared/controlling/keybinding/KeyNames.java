package com.blamejared.controlling.keybinding;

import net.minecraft.client.settings.GameSettings;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.api.ControllingApi;

import it.unimi.dsi.fastutil.ints.IntList;

/** Display names for keycodes, covering the mouse-button encoding and compact side-aware modifier names. */
public final class KeyNames {

    private KeyNames() {}

    public static String display(int keyCode) {
        if (ControllingApi.isMouseKeyCode(keyCode)) {
            final int button = keyCode - ControllingApi.MOUSE_KEYCODE_OFFSET;
            switch (button) {
                case 0:
                    return "LMB";
                case 1:
                    return "RMB";
                case 2:
                    return "MMB";
                default:
                    return "MB" + button;
            }
        }
        return switch (keyCode) {
            case Keyboard.KEY_LCONTROL -> "LCtrl";
            case Keyboard.KEY_RCONTROL -> "RCtrl";
            case Keyboard.KEY_LSHIFT -> "LShift";
            case Keyboard.KEY_RSHIFT -> "RShift";
            case Keyboard.KEY_LMENU -> "LAlt";
            case Keyboard.KEY_RMENU -> "RAlt";
            case Keyboard.KEY_LMETA -> "LMeta";
            case Keyboard.KEY_RMETA -> "RMeta";
            default -> GameSettings.getKeyDisplayString(keyCode);
        };
    }

    /** Joins a key list as "A + B + C"; empty list yields an empty string. */
    public static String joinChord(IntList keys) {
        if (keys.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(display(keys.getInt(i)));
        }
        return sb.toString();
    }
}
