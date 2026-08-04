package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.ComboKeyBinding;

/**
 * Captures the complete state of a keybinding for undo/redo functionality. Stores both the main key code and combo keys
 * for restoration.
 */
public class KeyBindingSnapshot {

    private final KeyBinding keyBinding;
    private final int keyCode;
    private final List<Integer> comboKeys;

    /**
     * Creates a snapshot of the current state of the given keybinding.
     * 
     * @param keyBinding The keybinding to snapshot
     */
    public KeyBindingSnapshot(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
        this.keyCode = keyBinding.getKeyCode();

        // Capture combo keys if this is a ComboKeyBinding
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            this.comboKeys = new ArrayList<>(comboKeyBinding.controlling$getComboKeys());
        } else {
            this.comboKeys = new ArrayList<>();
        }
    }

    /**
     * Restores the keybinding to the state captured in this snapshot.
     */
    public void restore() {
        // Restore the main key code
        keyBinding.setKeyCode(keyCode);

        // Restore combo keys if this is a ComboKeyBinding
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            comboKeyBinding.controlling$setComboKeys(comboKeys);
        }
    }

    /**
     * Gets the keybinding associated with this snapshot.
     * 
     * @return The keybinding
     */
    public KeyBinding getKeyBinding() {
        return keyBinding;
    }

    /**
     * Gets the key code stored in this snapshot.
     * 
     * @return The key code
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Gets a copy of the combo keys stored in this snapshot.
     * 
     * @return A list of combo keys
     */
    public List<Integer> getComboKeys() {
        return new ArrayList<>(comboKeys);
    }

    /**
     * Checks if the current state of the keybinding matches this snapshot.
     * 
     * @return true if the keybinding's current state is identical to this snapshot
     */
    public boolean matchesCurrentState() {
        // Check if key code matches
        if (keyBinding.getKeyCode() != keyCode) {
            return false;
        }

        // Check combo keys if this is a ComboKeyBinding
        if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            List<Integer> currentComboKeys = comboKeyBinding.controlling$getComboKeys();
            return comboKeys.equals(currentComboKeys);
        }

        return true;
    }

    @Override
    public String toString() {
        return "KeyBindingSnapshot{" + "keyBinding="
                + keyBinding.getKeyDescription()
                + ", keyCode="
                + keyCode
                + ", comboKeys="
                + comboKeys
                + '}';
    }
}
