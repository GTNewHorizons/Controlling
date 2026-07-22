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

import com.blamejared.controlling.api.KeyContext;
import com.blamejared.controlling.api.KeyContexts;
import com.blamejared.controlling.keybinding.ComboKeyBinding;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiNewKeyBindingList extends GuiKeyBindingList {

    private static final int YELLOW_HIGHLIGHT_COLOR = 0xFFDFD407;
    private static final int DARK_TEXT_HIGHLIGHT_COLOR = 0x404040;
    private static final int CONTEXT_COLOR_IN_GAME = 0xFF55DD55;
    private static final int CONTEXT_COLOR_GUI = 0xFF55AAFF;
    private static final int CONTEXT_COLOR_CUSTOM = 0xFFBB55FF;
    private static final int LOCK_COLOR = 0xFFD0D0D0;
    private static final int GLYPH_COLOR = 0xFFD0D0D0;
    private static final int GLYPH_DARK = 0xFF202020;
    private static final int SLASH_COLOR = 0xFFE04040;

    private final GuiNewControls controlsScreen;
    private final Minecraft mc;
    private final List<IGuiListEntry> displayedEntries = new ArrayList<>();
    private final List<IGuiListEntry> allEntries = new ArrayList<>();
    private int maxListLabelWidth;
    private String hoveredKeyDescription;
    private List<String> hoveredConflictLines;
    private String hoveredIndicatorText;

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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.hoveredKeyDescription = null;
        this.hoveredConflictLines = null;
        this.hoveredIndicatorText = null;
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public void drawHoveredKeyDescriptionTooltip(int mouseX, int mouseY) {
        if (this.hoveredKeyDescription != null) {
            this.controlsScreen.drawKeyDescriptionTooltip(this.hoveredKeyDescription, mouseX, mouseY);
        }
    }

    public void drawHoveredConflictTooltip(int mouseX, int mouseY) {
        if (this.hoveredConflictLines != null && !this.hoveredConflictLines.isEmpty()) {
            this.controlsScreen.drawConflictTooltip(this.hoveredConflictLines, mouseX, mouseY);
        }
    }

    public void drawHoveredIndicatorTooltip(int mouseX, int mouseY) {
        if (this.hoveredIndicatorText != null) {
            this.controlsScreen.drawKeyDescriptionTooltip(this.hoveredIndicatorText, mouseX, mouseY);
        }
    }

    private static int contextColor(KeyContext context) {
        if (context == KeyContexts.IN_GAME) {
            return CONTEXT_COLOR_IN_GAME;
        }
        if (context == KeyContexts.GUI) {
            return CONTEXT_COLOR_GUI;
        }
        return CONTEXT_COLOR_CUSTOM;
    }

    private static String contextDisplayName(KeyContext context) {
        if (context instanceof KeyContexts) {
            return I18n.format("options.context." + context.id());
        }
        return context.id();
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return this.displayedEntries.get(index);
    }

    @Override
    protected int getScrollBarX() {
        return super.getScrollBarX() + 15 + 20 + 14;
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

    public boolean scrollToKeyBinding(KeyBinding keyBinding) {
        for (int i = 0; i < this.displayedEntries.size(); i++) {
            if (this.displayedEntries.get(i) instanceof KeyEntry keyEntry && keyEntry.getKeybinding() == keyBinding) {
                int target = i * this.getSlotHeight() - (this.bottom - this.top) / 2 + this.getSlotHeight() / 2;
                this.scrollBy(target - this.getAmountScrolled());
                return true;
            }
        }
        return false;
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
        private final GuiButton btnVisualKeyboard;
        private final GuiButton btnResetKeyBinding;

        private KeyEntry(final KeyBinding keyBinding, CategoryEntry categoryEntry) {
            this.keybinding = keyBinding;
            this.keyDesc = I18n.format(keyBinding.getKeyDescription());
            this.btnChangeKeyBinding = new GuiButton(2000, 0, 0, 75 + 20, 20, this.keyDesc);
            this.btnVisualKeyboard = new GuiButton(2002, 0, 0, 18, 20, "");
            this.categoryEntry = categoryEntry;
            this.btnResetKeyBinding = new GuiButton(2001, 0, 0, 50, 20, I18n.format("controls.reset"));
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator,
                int mouseX, int mouseY, boolean isSelected) {
            boolean isKeySelected = controlsScreen.buttonId == this.keybinding;
            this.btnChangeKeyBinding.xPosition = x + 105;
            this.btnChangeKeyBinding.yPosition = y;
            this.drawKeyDescription(x, y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2, mouseX, mouseY);
            this.drawIndicators(x, y, mouseX, mouseY);

            this.btnVisualKeyboard.xPosition = x + 204;
            this.btnVisualKeyboard.yPosition = y;
            this.btnVisualKeyboard.drawButton(mc, mouseX, mouseY);
            this.drawKeyboardIcon();

            this.btnResetKeyBinding.xPosition = x + 224;
            this.btnResetKeyBinding.yPosition = y;
            this.btnResetKeyBinding.enabled = keybinding instanceof ComboKeyBinding comboKeyBinding
                    ? !comboKeyBinding.controlling$isSetToDefaultValue()
                    : this.keybinding.getKeyCode() != this.keybinding.getKeyCodeDefault();
            this.btnResetKeyBinding.drawButton(mc, mouseX, mouseY);

            this.btnChangeKeyBinding.displayString = keybinding instanceof ComboKeyBinding comboKeyBinding
                    ? comboKeyBinding.controlling$getDisplayName()
                    : GameSettings.getKeyDisplayString(this.keybinding.getKeyCode());

            boolean hasConflict = false;
            boolean modConflict = true; // less severe form of conflict, like SHIFT conflicting with SHIFT+G
            final List<KeyBinding> conflicts = new ArrayList<>();

            if (this.keybinding.getKeyCode() != 0) {
                for (KeyBinding key : mc.gameSettings.keyBindings) {
                    if (key != this.keybinding) {
                        if (key instanceof ComboKeyBinding comboKeyBinding
                                && keybinding instanceof ComboKeyBinding comboKeyBinding2) {
                            if (comboKeyBinding.controlling$conflicts(keybinding)) {
                                hasConflict = true;
                                modConflict &= comboKeyBinding2.controlling$hasKeyCodeModifierConflict(key);
                                conflicts.add(key);
                            }
                        } else if (this.keybinding.getKeyCode() == key.getKeyCode()) {
                            hasConflict = true;
                            conflicts.add(key);
                            break;
                        }
                    }
                }
            }

            if (hasConflict && !conflicts.isEmpty() && this.isMouseOverChangeButton(mouseX, mouseY)) {
                hoveredConflictLines = this.buildConflictTooltip(conflicts);
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

        private void drawKeyboardIcon() {
            int color = 0xFFFFFFFF;
            int left = this.btnVisualKeyboard.xPosition + 4;
            int top = this.btnVisualKeyboard.yPosition + 5;
            Gui.drawRect(left, top, left + 10, top + 1, color);
            Gui.drawRect(left, top + 8, left + 10, top + 9, color);
            Gui.drawRect(left, top, left + 1, top + 9, color);
            Gui.drawRect(left + 9, top, left + 10, top + 9, color);
            Gui.drawRect(left + 2, top + 2, left + 3, top + 3, color);
            Gui.drawRect(left + 4, top + 2, left + 6, top + 3, color);
            Gui.drawRect(left + 7, top + 2, left + 8, top + 3, color);
            Gui.drawRect(left + 2, top + 4, left + 3, top + 5, color);
            Gui.drawRect(left + 4, top + 4, left + 6, top + 5, color);
            Gui.drawRect(left + 7, top + 4, left + 8, top + 5, color);
            Gui.drawRect(left + 2, top + 6, left + 8, top + 7, color);
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

        private boolean isMouseOverChangeButton(int mouseX, int mouseY) {
            final GuiButton btn = this.btnChangeKeyBinding;
            return mouseX >= btn.xPosition && mouseX < btn.xPosition + btn.width
                    && mouseY >= btn.yPosition
                    && mouseY < btn.yPosition + btn.height;
        }

        private List<String> buildConflictTooltip(List<KeyBinding> conflicts) {
            final List<String> lines = new ArrayList<>();
            lines.add(I18n.format("options.conflictsHeader"));
            final ComboKeyBinding self = this.keybinding instanceof ComboKeyBinding combo ? combo : null;
            for (KeyBinding conflict : conflicts) {
                final boolean modifierConflict = self != null && self.controlling$hasKeyCodeModifierConflict(conflict);
                final EnumChatFormatting color = modifierConflict ? EnumChatFormatting.GOLD : EnumChatFormatting.RED;
                final String name = I18n.format(conflict.getKeyDescription());
                final String keyDisplay = conflict instanceof ComboKeyBinding combo ? combo.controlling$getDisplayName()
                        : GameSettings.getKeyDisplayString(conflict.getKeyCode());
                lines.add(color + name + EnumChatFormatting.GRAY + "  [" + keyDisplay + "]");
            }
            return lines;
        }

        private void drawIndicators(int x, int y, int mouseX, int mouseY) {
            final ComboKeyBinding combo = this.keybinding instanceof ComboKeyBinding c ? c : null;
            final KeyContext context = combo != null ? combo.controlling$getKeyContext() : KeyContexts.UNIVERSAL;
            final boolean lockedModifier = combo != null && !combo.controlling$allowsComboModifier();
            final boolean noMouse = combo != null && !combo.controlling$allowsMouse();
            final boolean noKeyboard = combo != null && !combo.controlling$allowsKeyboard();

            // 2x2 grid in the widened gap: reset ends at x + 274, scroll bar at x + 297.
            final int colLeft = x + 275;
            final int colRight = x + 285;
            final int rowTop = y + 2;
            final int rowBottom = y + 11;

            if (context != KeyContexts.UNIVERSAL) {
                this.drawContextDot(colLeft, rowTop, contextColor(context));
                this.setIndicatorHover(
                        mouseX,
                        mouseY,
                        colLeft,
                        rowTop,
                        I18n.format("options.contextLabel", contextDisplayName(context)));
            }
            if (lockedModifier) {
                this.drawLockIcon(colRight + 1, rowTop);
                this.setIndicatorHover(mouseX, mouseY, colRight, rowTop, I18n.format("options.modifiersLocked"));
            }
            if (noMouse) {
                this.drawMouseGlyph(colLeft, rowBottom);
                this.setIndicatorHover(mouseX, mouseY, colLeft, rowBottom, I18n.format("options.mouseDisabled"));
            }
            if (noKeyboard) {
                this.drawKeyboardGlyph(colRight, rowBottom);
                this.setIndicatorHover(mouseX, mouseY, colRight, rowBottom, I18n.format("options.keyboardDisabled"));
            }
        }

        private void setIndicatorHover(int mouseX, int mouseY, int left, int top, String text) {
            if (mouseX >= left && mouseX < left + 9 && mouseY >= top && mouseY < top + 9) {
                hoveredIndicatorText = text;
            }
        }

        private void drawContextDot(int left, int top, int color) {
            Gui.drawRect(left + 2, top + 2, left + 7, top + 7, color); // 5x5 centered in the cell
        }

        private void drawLockIcon(int left, int top) {
            Gui.drawRect(left + 1, top, left + 5, top + 1, LOCK_COLOR); // shackle top
            Gui.drawRect(left + 1, top, left + 2, top + 3, LOCK_COLOR); // shackle left
            Gui.drawRect(left + 4, top, left + 5, top + 3, LOCK_COLOR); // shackle right
            Gui.drawRect(left, top + 3, left + 6, top + 8, LOCK_COLOR); // body
        }

        private void drawMouseGlyph(int left, int top) {
            final int l = left + 2;
            final int t = top + 1;
            Gui.drawRect(l + 1, t, l + 4, t + 7, GLYPH_COLOR); // body
            Gui.drawRect(l, t + 1, l + 5, t + 6, GLYPH_COLOR); // rounded sides
            Gui.drawRect(l + 2, t, l + 3, t + 3, GLYPH_DARK); // button split
            this.drawSlash(left, top);
        }

        private void drawKeyboardGlyph(int left, int top) {
            final int l = left + 1;
            final int t = top + 2;
            Gui.drawRect(l, t, l + 7, t + 5, GLYPH_COLOR); // outer
            Gui.drawRect(l + 1, t + 1, l + 6, t + 4, GLYPH_DARK); // inner
            Gui.drawRect(l + 1, t + 1, l + 2, t + 2, GLYPH_COLOR); // key
            Gui.drawRect(l + 3, t + 1, l + 4, t + 2, GLYPH_COLOR); // key
            Gui.drawRect(l + 5, t + 1, l + 6, t + 2, GLYPH_COLOR); // key
            Gui.drawRect(l + 2, t + 2, l + 5, t + 3, GLYPH_COLOR); // space bar
            this.drawSlash(left, top);
        }

        private void drawSlash(int left, int top) {
            Gui.drawRect(left, top + 6, left + 2, top + 8, SLASH_COLOR);
            Gui.drawRect(left + 2, top + 4, left + 4, top + 6, SLASH_COLOR);
            Gui.drawRect(left + 4, top + 2, left + 6, top + 4, SLASH_COLOR);
            Gui.drawRect(left + 6, top, left + 8, top + 2, SLASH_COLOR);
        }

        private void drawKeyDescription(int x, int y, int mouseX, int mouseY) {
            final int labelLeft = 4;
            final int labelRight = x + 95;
            final int maxWidth = Math.max(0, labelRight - labelLeft);
            final boolean truncated = mc.fontRenderer.getStringWidth(this.keyDesc) > maxWidth;
            final String label = trimWithEllipsis(this.keyDesc, maxWidth);
            final int labelX = labelRight - mc.fontRenderer.getStringWidth(label);
            drawHighlightedString(label, labelX, y, shouldHighlightKeybindName());
            if (truncated && mouseX >= labelLeft
                    && mouseX <= labelRight
                    && mouseY >= y
                    && mouseY < y + mc.fontRenderer.FONT_HEIGHT) {
                hoveredKeyDescription = this.keyDesc;
            }
        }

        private String trimWithEllipsis(String text, int width) {
            if (mc.fontRenderer.getStringWidth(text) <= width) {
                return text;
            }
            final String ellipsis = "...";
            final int ellipsisWidth = mc.fontRenderer.getStringWidth(ellipsis);
            return width <= ellipsisWidth ? mc.fontRenderer.trimStringToWidth(text, width)
                    : mc.fontRenderer.trimStringToWidth(text, width - ellipsisWidth) + ellipsis;
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX,
                int relativeY) {
            if (this.btnChangeKeyBinding.mousePressed(mc, mouseX, mouseY)) {
                controlsScreen.selectKeyBinding(this.keybinding, false);
                return true;
            } else if (this.btnVisualKeyboard.mousePressed(mc, mouseX, mouseY)) {
                controlsScreen.selectKeyBinding(this.keybinding, true);
                return true;
            } else if (this.btnResetKeyBinding.mousePressed(mc, mouseX, mouseY)) {
                if (keybinding instanceof ComboKeyBinding comboKeyBinding) {
                    comboKeyBinding.controlling$setToDefault();
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
            this.btnVisualKeyboard.mouseReleased(x, y);
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
