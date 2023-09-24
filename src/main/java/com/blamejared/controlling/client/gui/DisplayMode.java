package com.blamejared.controlling.client.gui;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public enum DisplayMode {

    ALL,
    CONFLICTING,
    UNBOUND;

    public Predicate<GuiNewKeyBindingList.KeyEntry> getPredicate() {
        switch (this) {
            case ALL:
                return keyEntry -> true;
            case CONFLICTING:
                return keyEntry -> {
                    for (KeyBinding key : Minecraft.getMinecraft().gameSettings.keyBindings) {
                        if (!key.getKeyDescription().equals(keyEntry.getKeybinding().getKeyDescription())
                                && key.getKeyCode() != 0) {
                            if (key.getKeyCode() == keyEntry.getKeybinding().getKeyCode()) {
                                return true;
                            }
                        }
                    }
                    return false;
                };
            case UNBOUND:
                return keyEntry -> keyEntry.getKeybinding().getKeyCode() == 0;
        }
        throw new IllegalStateException();
    }

}
