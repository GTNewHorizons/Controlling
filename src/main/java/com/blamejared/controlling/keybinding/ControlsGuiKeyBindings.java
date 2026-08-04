package com.blamejared.controlling.keybinding;

import java.util.Arrays;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.api.KeyContexts;

import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * Keybindings for the controls GUI (undo/redo/search).
 */
public final class ControlsGuiKeyBindings {

    private static final String CATEGORY = "key.categories.controlling";

    public static KeyBinding UNDO;
    public static KeyBinding REDO;
    public static KeyBinding SEARCH;

    private ControlsGuiKeyBindings() {}

    /**
     * Registers the undo, redo, and search keybindings for the controls GUI.
     */
    public static void register() {

        UNDO = new KeyBinding("key.controlling.undo", Keyboard.KEY_Z, CATEGORY);
        ClientRegistry.registerKeyBinding(UNDO);
        ControllingApi.setKeyConflictContext(UNDO, KeyContexts.SETTINGS);
        ControllingApi.setAllowsChords(UNDO, true);
        ControllingApi.setDefaultComboKeys(UNDO, Arrays.asList(Keyboard.KEY_LCONTROL));

        REDO = new KeyBinding("key.controlling.redo", Keyboard.KEY_Y, CATEGORY);
        ClientRegistry.registerKeyBinding(REDO);
        ControllingApi.setKeyConflictContext(REDO, KeyContexts.SETTINGS);
        ControllingApi.setAllowsChords(REDO, true);
        ControllingApi.setDefaultComboKeys(REDO, Arrays.asList(Keyboard.KEY_LCONTROL));

        SEARCH = new KeyBinding("key.controlling.search", Keyboard.KEY_F, CATEGORY);
        ClientRegistry.registerKeyBinding(SEARCH);
        ControllingApi.setKeyConflictContext(SEARCH, KeyContexts.SETTINGS);
        ControllingApi.setAllowsChords(SEARCH, true);
        ControllingApi.setDefaultComboKeys(SEARCH, Arrays.asList(Keyboard.KEY_LCONTROL));
    }
}
