package com.blamejared.controlling.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiListExtended;

public interface ISort {
    void sort(List<GuiListExtended.IGuiListEntry> entries);
}
