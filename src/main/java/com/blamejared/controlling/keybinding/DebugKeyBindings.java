package com.blamejared.controlling.keybinding;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.launchwrapper.Launch;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.api.KeyContext;
import com.blamejared.controlling.api.KeyContexts;

import cpw.mods.fml.client.registry.ClientRegistry;

public final class DebugKeyBindings {

    private static final String CATEGORY = "Controlling Debug";

    /** Custom conflict context; conflicts only with itself (plus UNIVERSAL via the a||b rule). */
    private static final KeyContext DEBUG_CONTEXT = new KeyContext() {

        @Override
        public String id() {
            return "controlling_debug";
        }

        @Override
        public boolean conflicts(KeyContext other) {
            return other == this;
        }
    };

    private DebugKeyBindings() {}

    public static boolean isDevEnvironment() {
        final Object flag = Launch.blackboard == null ? null : Launch.blackboard.get("fml.deobfuscatedEnvironment");
        return flag instanceof Boolean && (Boolean) flag;
    }

    /** Registers the debug keybindings only when running in a dev environment. */
    public static void registerIfDevEnvironment() {
        if (!isDevEnvironment()) {
            return;
        }
        ControllingApi.registerKeyContext(DEBUG_CONTEXT);

        // Three on U: UNIVERSAL flags against both; IN_GAME and GUI do not flag each other.
        registerBind("Debug: Universal", Keyboard.KEY_U, KeyContexts.UNIVERSAL, true);
        registerBind("Debug: In-Game", Keyboard.KEY_U, KeyContexts.IN_GAME, true);
        registerBind("Debug: GUI", Keyboard.KEY_U, KeyContexts.GUI, true);
        // Cannot have a combo modifier attached in the controls screen or visual keyboard.
        registerBind("Debug: No Modifier", Keyboard.KEY_J, KeyContexts.UNIVERSAL, false);
        // Exercises the registry and a custom conflict context.
        registerBind("Debug: Custom Ctx", Keyboard.KEY_K, DEBUG_CONTEXT, true);

        // Input-type opt-outs.
        final KeyBinding noMouse = registerBind("Debug: No Mouse", Keyboard.KEY_M, KeyContexts.UNIVERSAL, true);
        ControllingApi.setAllowsMouse(noMouse, false);
        final KeyBinding noKeyboard = registerBind("Debug: No Keyboard", Keyboard.KEY_N, KeyContexts.UNIVERSAL, true);
        ControllingApi.setAllowsKeyboard(noKeyboard, false);
        // Chords allowed, but never on Ctrl: exercises the per-key block list.
        final KeyBinding noCtrl = registerBind("Debug: No Ctrl Chord", Keyboard.KEY_B, KeyContexts.UNIVERSAL, true);
        ControllingApi.setBlockedChordKeys(noCtrl, Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL);

        final KeyBinding allTest = registerBind("Debug: Multiple", Keyboard.KEY_Y, KeyContexts.GUI, false);
        ControllingApi.setAllowsKeyboard(allTest, false);
        ControllingApi.setAllowsMouse(allTest, false);
    }

    private static KeyBinding registerBind(String description, int keyCode, KeyContext context, boolean allowsChords) {
        final KeyBinding keyBinding = new KeyBinding(description, keyCode, CATEGORY);
        ClientRegistry.registerKeyBinding(keyBinding);
        ControllingApi.setKeyConflictContext(keyBinding, context);
        ControllingApi.setAllowsChords(keyBinding, allowsChords);
        return keyBinding;
    }
}
