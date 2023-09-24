package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumChatFormatting;

import org.apache.commons.lang3.ArrayUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiNewKeyBindingList extends GuiKeyBindingList {

    private final GuiControls controlsScreen;
    private final Minecraft mc;
    private final List<IGuiListEntry> displayedEntries = new ArrayList<>();
    private final List<IGuiListEntry> allEntries = new ArrayList<>();
    private int maxListLabelWidth;

    public GuiNewKeyBindingList(GuiControls controls, Minecraft mcIn) {
        super(controls, mcIn);
        this.width = controls.width + 45;
        this.height = controls.height;
        this.top = 63;
        this.bottom = controls.height - 80;
        this.right = controls.width + 45;
        this.controlsScreen = controls;
        this.mc = mcIn;

        KeyBinding[] keyBindings = ArrayUtils.clone(mcIn.gameSettings.keyBindings);
        Arrays.sort(keyBindings);
        String prevCategory = null;
        CategoryEntry prevCategoryEntry = null;

        for (KeyBinding keybinding : keyBindings) {
            String category = keybinding.getKeyCategory();
            if (category.endsWith(".hidden")) continue;
            if (!category.equals(prevCategory)) {
                prevCategory = category;
                prevCategoryEntry = new CategoryEntry(category);
                allEntries.add(prevCategoryEntry);
            }
            maxListLabelWidth = Math.max(
                    maxListLabelWidth,
                    mcIn.fontRenderer.getStringWidth(I18n.format(keybinding.getKeyDescription())));
            allEntries.add(new KeyEntry(keybinding, prevCategoryEntry));
        }

        displayedEntries.addAll(allEntries);
    }

    @Override
    protected int getSize() {
        return this.displayedEntries.size();
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return this.displayedEntries.get(index);
    }

    public List<IGuiListEntry> getAllEntries() {
        return allEntries;
    }

    @Override
    protected int getScrollBarX() {
        return super.getScrollBarX() + 15 + 20;
    }

    @Override
    public int getListWidth() {
        return super.getListWidth() + 32;
    }

    @SideOnly(Side.CLIENT)
    public class CategoryEntry implements IGuiListEntry {

        private final String labelText;
        private final int labelWidth;
        private final String name;

        public CategoryEntry(String name) {
            this.labelText = I18n.format(name);
            this.labelWidth = mc.fontRenderer.getStringWidth(this.labelText);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator,
                int mouseX, int mouseY, boolean isSelected) {
            mc.fontRenderer.drawString(
                    this.labelText,
                    mc.currentScreen.width / 2 - this.labelWidth / 2,
                    y + slotHeight - mc.fontRenderer.FONT_HEIGHT - 1,
                    0xFFFFFF);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX,
                int relativeY) {
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {}
    }

    @SideOnly(Side.CLIENT)
    public class KeyEntry implements IGuiListEntry {

        /**
         * The keybinding specified for this KeyEntry
         */
        private final KeyBinding keybinding;
        /**
         * The localized key description for this KeyEntry
         */
        private final String keyDesc;
        private final CategoryEntry categoryEntry;

        private final GuiButton btnChangeKeyBinding;
        private final GuiButton btnResetKeyBinding;

        private KeyEntry(final KeyBinding keyBinding, CategoryEntry categoryEntry) {
            this.keybinding = keyBinding;
            this.keyDesc = I18n.format(keyBinding.getKeyDescription());
            this.btnChangeKeyBinding = new GuiButton(2000, 0, 0, 75 + 20, 20, this.keyDesc);
            this.categoryEntry = categoryEntry;
            this.btnResetKeyBinding = new GuiButton(2001, 0, 0, 50, 20, I18n.format("controls.reset"));
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator,
                int mouseX, int mouseY, boolean isSelected) {
            boolean isKeySelected = controlsScreen.buttonId == this.keybinding;
            mc.fontRenderer.drawString(
                    this.keyDesc,
                    x + 90 - maxListLabelWidth,
                    y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2,
                    0xFFFFFF);
            this.btnResetKeyBinding.xPosition = x + 190 + 20;
            this.btnResetKeyBinding.yPosition = y;
            this.btnResetKeyBinding.enabled = !(this.keybinding.getKeyCode() == this.keybinding.getKeyCodeDefault());
            this.btnResetKeyBinding.drawButton(mc, mouseX, mouseY);

            this.btnChangeKeyBinding.xPosition = x + 105;
            this.btnChangeKeyBinding.yPosition = y;
            this.btnChangeKeyBinding.displayString = GameSettings.getKeyDisplayString(this.keybinding.getKeyCode());

            boolean hasConflict = false;

            if (this.keybinding.getKeyCode() != 0) {
                for (KeyBinding key : mc.gameSettings.keyBindings) {
                    if (key != this.keybinding && this.keybinding.getKeyCode() == key.getKeyCode()) {
                        hasConflict = true;
                        break;
                    }
                }
            }

            if (isKeySelected) {
                this.btnChangeKeyBinding.displayString = EnumChatFormatting.YELLOW + "> "
                        + EnumChatFormatting.RESET
                        + EnumChatFormatting.UNDERLINE
                        + this.btnChangeKeyBinding.displayString
                        + EnumChatFormatting.RESET
                        + EnumChatFormatting.YELLOW
                        + " <";
            } else if (hasConflict) {
                this.btnChangeKeyBinding.displayString = EnumChatFormatting.RED + "[ "
                        + EnumChatFormatting.RESET
                        + this.btnChangeKeyBinding.displayString
                        + EnumChatFormatting.RED
                        + " ]";
            }

            this.btnChangeKeyBinding.drawButton(mc, mouseX, mouseY);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX,
                int relativeY) {
            if (this.btnChangeKeyBinding.mousePressed(mc, mouseX, mouseY)) {
                controlsScreen.buttonId = this.keybinding;
                return true;
            } else if (this.btnResetKeyBinding.mousePressed(mc, mouseX, mouseY)) {
                this.keybinding.setKeyCode(this.keybinding.getKeyCodeDefault());
                mc.gameSettings.setOptionKeyBinding(this.keybinding, this.keybinding.getKeyCodeDefault());
                KeyBinding.resetKeyBindingArrayAndHash();
                return true;
            }

            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            this.btnChangeKeyBinding.mouseReleased(x, y);
            this.btnResetKeyBinding.mouseReleased(x, y);
        }

        public KeyBinding getKeybinding() {
            return keybinding;
        }

        public String getKeyDesc() {
            return keyDesc;
        }

        public CategoryEntry getCategoryEntry() {
            return categoryEntry;
        }
    }

    public void setDisplayedEntries(List<IGuiListEntry> displayedEntries) {
        this.displayedEntries.clear();
        this.displayedEntries.addAll(displayedEntries);
    }
}
