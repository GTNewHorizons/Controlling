package com.blamejared.controlling.client.gui;

import java.util.Comparator;
import java.util.List;

import net.minecraft.client.resources.I18n;

public enum SortOrder {

    VANILLA,
    AZ,
    ZA;

    public static final SortOrder[] VALUES = values();

    public SortOrder getNext() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public void sort(List<GuiNewKeyBindingList.KeyEntry> list) {
        switch (this) {
            case VANILLA:
                return;
            case AZ:
                list.sort(Comparator.comparing(GuiNewKeyBindingList.KeyEntry::getKeyDesc));
                return;
            case ZA:
                list.sort(
                        Comparator.comparing(entry -> ((GuiNewKeyBindingList.KeyEntry) entry).getKeyDesc()).reversed());
        }
    }

    public String getName() {
        switch (this) {
            case VANILLA:
                return I18n.format("options.sortNone");
            case AZ:
                return I18n.format("options.sortAZ");
            case ZA:
                return I18n.format("options.sortZA");
        }
        throw new IllegalStateException();
    }

    public String getNextName() {
        return this.getNext().getName();
    }

}
