package com.blamejared.controlling.client.gui;

import java.util.function.Predicate;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.StatCollector;

import com.blamejared.controlling.keybinding.ComboKeyBinding;

public enum SearchType {

    ALL,
    CATEGORY_NAME,
    KEYBIND_NAME,
    KEY_NAME;

    public Predicate<GuiNewKeyBindingList.KeyEntry> getPredicate(String searchText) {
        switch (this) {
            case ALL:
                return CATEGORY_NAME.getPredicate(searchText).or(KEYBIND_NAME.getPredicate(searchText))
                        .or(KEY_NAME.getPredicate(searchText));
            case CATEGORY_NAME:
                return key -> StatCollector.translateToLocal(key.getKeybinding().getKeyCategory()).toLowerCase()
                        .contains(searchText.toLowerCase());
            case KEYBIND_NAME:
                return key -> key.getKeyDesc().toLowerCase().contains(searchText.toLowerCase());
            case KEY_NAME:
                return key -> {
                    final String displayName = key.getKeybinding() instanceof ComboKeyBinding comboKeyBinding
                            ? comboKeyBinding.controlling$getDisplayName()
                            : GameSettings.getKeyDisplayString(key.getKeybinding().getKeyCode());
                    return displayName.toLowerCase().contains(searchText.toLowerCase());
                };
        }
        throw new IllegalStateException();
    }

}
