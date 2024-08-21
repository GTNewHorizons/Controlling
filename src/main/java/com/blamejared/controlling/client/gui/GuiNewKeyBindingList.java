package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumChatFormatting;

import org.apache.commons.lang3.ArrayUtils;

import com.blamejared.controlling.Controlling;
import committee.nova.mkb.api.IKeyBinding;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiNewKeyBindingList extends GuiKeyBindingList {

    private static final int YELLOW_HIGHLIGHT_COLOR = 0xFFDFD407;
    private static final int DARK_TEXT_HIGHLIGHT_COLOR = 0x404040;

    private final GuiNewControls controlsScreen;
    private final Minecraft mc;
    private final List<IGuiListEntry> displayedEntries = new ArrayList<>();
    private final List<IGuiListEntry> allEntries = new ArrayList<>();
    private int maxListLabelWidth;

    public GuiNewKeyBindingList(GuiNewControls controls, Minecraft mcIn) {
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

    @Override
    protected int getScrollBarX() {
        return super.getScrollBarX() + 15 + 20;
    }

    @Override
    public int getListWidth() {
        return super.getListWidth() + 32;
    }

    public List<IGuiListEntry> getAllEntries() {
        return allEntries;
    }

    public void setDisplayedEntries(List<IGuiListEntry> displayedEntries) {
        this.displayedEntries.clear();
        this.displayedEntries.addAll(displayedEntries);
    }

    private void drawHighlightedString(String text, int x, int y, boolean highlight) {
        final int index = text.toLowerCase().indexOf(controlsScreen.getSearchString().toLowerCase());
        if (highlight && index > -1) {
            final int indexEndHighlight = index + controlsScreen.getSearchString().length();
            final String textStart = text.substring(0, index);
            final String textMiddle = text.substring(index, indexEndHighlight);
            final String textEnd = text.substring(indexEndHighlight);
            final int rectLeft = x + mc.fontRenderer.getStringWidth(textStart);
            final int rectRight = rectLeft + mc.fontRenderer.getStringWidth(textMiddle);
            final int rectBottom = y + mc.fontRenderer.FONT_HEIGHT;
            Gui.drawRect(rectLeft, y, rectRight, rectBottom, YELLOW_HIGHLIGHT_COLOR);
            mc.fontRenderer.drawStringWithShadow(textStart, x, y, 0xFFFFFF);
            mc.fontRenderer.drawString(textMiddle, rectLeft, y, DARK_TEXT_HIGHLIGHT_COLOR);
            mc.fontRenderer.drawStringWithShadow(textEnd, rectRight, y, 0xFFFFFF);
        } else {
            mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFF);
        }
    }

    private boolean shouldHighlightCategoryName() {
        return !controlsScreen.getSearchString().isEmpty() && (controlsScreen.getSearchType() == SearchType.ALL
                || controlsScreen.getSearchType() == SearchType.CATEGORY_NAME);
    }

    private boolean shouldHighlightKeybindName() {
        return !controlsScreen.getSearchString().isEmpty() && (controlsScreen.getSearchType() == SearchType.ALL
                || controlsScreen.getSearchType() == SearchType.KEYBIND_NAME);
    }

    private boolean shouldHighlightKeyName() {
        return !controlsScreen.getSearchString().isEmpty() && (controlsScreen.getSearchType() == SearchType.ALL
                || controlsScreen.getSearchType() == SearchType.KEY_NAME);
    }

    @SideOnly(Side.CLIENT)
    public class CategoryEntry implements IGuiListEntry {

        private final String labelText;
        private final int labelWidth;

        public CategoryEntry(String name) {
            this.labelText = I18n.format(name);
            this.labelWidth = mc.fontRenderer.getStringWidth(this.labelText);
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator,
                int mouseX, int mouseY, boolean isSelected) {
            drawHighlightedString(
                    this.labelText,
                    mc.currentScreen.width / 2 - this.labelWidth / 2,
                    y + slotHeight - mc.fontRenderer.FONT_HEIGHT - 1,
                    shouldHighlightCategoryName());
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
            drawHighlightedString(
                    this.keyDesc,
                    x + 90 - maxListLabelWidth,
                    y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2,
                    shouldHighlightKeybindName());
            this.btnResetKeyBinding.xPosition = x + 190 + 20;
            this.btnResetKeyBinding.yPosition = y;
            this.btnResetKeyBinding.enabled = !(this.keybinding.getKeyCode() == this.keybinding.getKeyCodeDefault());
            this.btnResetKeyBinding.drawButton(mc, mouseX, mouseY);

            this.btnChangeKeyBinding.xPosition = x + 105;
            this.btnChangeKeyBinding.yPosition = y;
            this.btnChangeKeyBinding.displayString = Controlling.isModernKeybindingInstalled
                    && keybinding instanceof IKeyBinding modernKB ? modernKB.getDisplayName()
                            : GameSettings.getKeyDisplayString(this.keybinding.getKeyCode());

            boolean hasConflict = false;
            boolean modConflict = true; // less severe form of conflict, like SHIFT conflicting with SHIFT+G

            if (this.keybinding.getKeyCode() != 0) {
                for (KeyBinding key : mc.gameSettings.keyBindings) {
                    if (key != this.keybinding) {
                        if (Controlling.isModernKeybindingInstalled && key instanceof IKeyBinding modernKB
                                && keybinding instanceof IKeyBinding modernKB2
                                && modernKB.conflicts(keybinding)) {
                            hasConflict = true;
                            modConflict &= modernKB2.hasKeyCodeModifierConflict(key);
                        } else if (this.keybinding.getKeyCode() == key.getKeyCode()) {
                            hasConflict = true;
                            break;
                        }
                    }
                }
            }

            final String displayText = this.btnChangeKeyBinding.displayString;
            final String searchString = controlsScreen.getSearchString();
            final int index = this.btnChangeKeyBinding.displayString.toLowerCase().indexOf(searchString.toLowerCase());
            final int indexEndHighlight = index + controlsScreen.getSearchString().length();
            final boolean highlight = shouldHighlightKeyName() && index > -1;
            String textStart = null;
            String textMiddle = null;
            String textEnd = null;
            if (highlight) {
                textStart = displayText.substring(0, index);
                textMiddle = displayText.substring(index, indexEndHighlight);
                textEnd = displayText.substring(indexEndHighlight);
            }

            String prefix = "";
            String suffix = "";
            if (isKeySelected) {
                prefix = EnumChatFormatting.YELLOW + "> " + EnumChatFormatting.RESET + EnumChatFormatting.UNDERLINE;
                suffix = EnumChatFormatting.RESET.toString() + EnumChatFormatting.YELLOW + " <";
            } else if (hasConflict) {
                EnumChatFormatting clr = modConflict ? EnumChatFormatting.GOLD : EnumChatFormatting.RED;
                prefix = clr + "[ " + EnumChatFormatting.RESET;
                suffix = clr + " ]";
            }
            this.btnChangeKeyBinding.displayString = prefix + this.btnChangeKeyBinding.displayString + suffix;

            if (highlight) {
                this.drawButtonWithHighlightedText(mouseX, mouseY, prefix, textStart, textMiddle, textEnd, suffix);
            } else {
                this.btnChangeKeyBinding.drawButton(mc, mouseX, mouseY);
            }

        }

        private void drawButtonWithHighlightedText(int mouseX, int mouseY, String prefix, String textStart,
                String textMiddle, String textEnd, String suffix) {
            if (prefix.contains(EnumChatFormatting.UNDERLINE.toString())) {
                textMiddle = EnumChatFormatting.UNDERLINE + textMiddle;
                textEnd = EnumChatFormatting.UNDERLINE + textEnd;
            }
            final String saveStr = this.btnChangeKeyBinding.displayString;
            this.btnChangeKeyBinding.displayString = "";
            this.btnChangeKeyBinding.drawButton(mc, mouseX, mouseY);
            this.btnChangeKeyBinding.displayString = saveStr;
            int xString = this.btnChangeKeyBinding.xPosition + this.btnChangeKeyBinding.width / 2
                    - mc.fontRenderer.getStringWidth(this.btnChangeKeyBinding.displayString) / 2;
            final int yString = this.btnChangeKeyBinding.yPosition + (this.btnChangeKeyBinding.height - 8) / 2;
            final String drawnTextStart = prefix + textStart;
            final int rectLeft = xString + mc.fontRenderer.getStringWidth(drawnTextStart);
            final int rectRight = rectLeft + mc.fontRenderer.getStringWidth(textMiddle);
            final int rectBottom = yString + mc.fontRenderer.FONT_HEIGHT;
            Gui.drawRect(rectLeft, yString, rectRight, rectBottom, YELLOW_HIGHLIGHT_COLOR);
            mc.fontRenderer.drawStringWithShadow(drawnTextStart, xString, yString, 0xFFFFFF);
            mc.fontRenderer.drawString(textMiddle, rectLeft, yString, DARK_TEXT_HIGHLIGHT_COLOR);
            mc.fontRenderer.drawStringWithShadow(textEnd + suffix, rectRight, yString, 0xFFFFFF);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX,
                int relativeY) {
            if (this.btnChangeKeyBinding.mousePressed(mc, mouseX, mouseY)) {
                controlsScreen.buttonId = this.keybinding;
                return true;
            } else if (this.btnResetKeyBinding.mousePressed(mc, mouseX, mouseY)) {
                if (Controlling.isModernKeybindingInstalled && keybinding instanceof IKeyBinding modernKB) {
                    modernKB.setToDefault();
                } else {
                    this.keybinding.setKeyCode(this.keybinding.getKeyCodeDefault());
                }
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

}
