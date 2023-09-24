package com.blamejared.controlling.client.gui;

import java.util.Comparator;
import java.util.List;

import net.minecraft.client.resources.I18n;

public enum SortOrder {

    VANILLA(entries -> {}),
    AZ(entries -> entries.sort(Comparator.comparing(entry -> entry.getKeybinding().getKeyDescription()))),
    ZA(entries -> entries.sort(
            Comparator.comparing(entry -> ((GuiNewKeyBindingList.KeyEntry) entry).getKeybinding().getKeyDescription())
                    .reversed()));

    private final ISort sorter;

    SortOrder(ISort sorter) {
        this.sorter = sorter;
    }

    public SortOrder cycle() {
        return SortOrder.values()[(this.ordinal() + 1) % SortOrder.values().length];
    }

    public void sort(List<GuiNewKeyBindingList.KeyEntry> list) {
        this.sorter.sort(list);
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

}
