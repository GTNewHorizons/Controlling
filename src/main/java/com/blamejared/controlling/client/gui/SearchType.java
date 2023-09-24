package com.blamejared.controlling.client.gui;

import java.util.function.Predicate;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.StatCollector;

public enum SearchType {

    NAME,
    KEY,
    CATEGORY;

    public Predicate<GuiNewKeyBindingList.KeyEntry> getPredicate(String searchText) {
        switch (this) {
            case NAME:
                return keyEntry -> keyEntry.getKeyDesc().toLowerCase().contains(searchText.toLowerCase());
            case CATEGORY:
                return keyEntry -> StatCollector.translateToLocal(keyEntry.getKeybinding().getKeyCategory())
                        .toLowerCase().contains(searchText.toLowerCase());
            case KEY:
                return keyEntry -> GameSettings.getKeyDisplayString(keyEntry.getKeybinding().getKeyCode()).toLowerCase()
                        .contains(searchText.toLowerCase());
        }
        throw new IllegalStateException();
    }

}
