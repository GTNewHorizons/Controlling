package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;

import com.blamejared.controlling.Controlling;
import com.blamejared.controlling.api.KeyContext;
import com.blamejared.controlling.api.KeyContexts;
import com.blamejared.controlling.config.ControllingConfig;
import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.KeyNames;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

@SideOnly(Side.CLIENT)
public class GuiNewKeyBindingList extends GuiKeyBindingList {

    private static final float MIN_LABEL_SCALE = 0.5F;
    private static final int YELLOW_HIGHLIGHT_COLOR = 0xFFDFD407;
    private static final int DARK_TEXT_HIGHLIGHT_COLOR = 0x404040;
    private static final int CONTEXT_BAR_WIDTH = 2;

    /**
     * Angelica renders &#RRGGBB color codes, and GTNHLib's font helpers treat them as zero width, so measuring and
     * trimming stay correct. Without Angelica the code would be drawn literally, so fall back to a vanilla color.
     */
    private static final boolean RGB_TEXT = Loader.isModLoaded("angelica");
    // Row markers are fixed per state, so they are built once rather than concatenated per row per frame. They also
    // depend on the palette, so refreshMarkers rebuilds them only when it changes; it runs once per frame.
    private static ColorPalette markerPalette;
    private static String MARK_SOFT_COLOR;
    private static String MARK_HARD_COLOR;
    private static String MARK_SELECTED_PREFIX;
    private static String MARK_SELECTED_SUFFIX;
    private static String MARK_SOFT_PREFIX;
    private static String MARK_SOFT_SUFFIX;
    private static String MARK_HARD_PREFIX;
    private static String MARK_HARD_SUFFIX;
    private static String MARK_SELECTED;
    private static String MARK_SOFT;
    private static String MARK_HARD;

    private static void refreshMarkers() {
        final ColorPalette p = ControllingConfig.palette;
        if (p == markerPalette) {
            return;
        }
        markerPalette = p;
        MARK_SOFT_COLOR = RGB_TEXT ? "&#" + p.markSoftRgb : p.markSoftFallback.toString();
        MARK_HARD_COLOR = RGB_TEXT ? "&#" + p.markHardRgb : p.markHardFallback.toString();
        MARK_SELECTED_PREFIX = EnumChatFormatting.YELLOW + "> "
                + EnumChatFormatting.RESET
                + EnumChatFormatting.UNDERLINE;
        MARK_SELECTED_SUFFIX = EnumChatFormatting.RESET.toString() + EnumChatFormatting.YELLOW + " <";
        MARK_SOFT_PREFIX = MARK_SOFT_COLOR + "[ " + EnumChatFormatting.RESET;
        MARK_SOFT_SUFFIX = MARK_SOFT_COLOR + " ]";
        MARK_HARD_PREFIX = MARK_HARD_COLOR + "[ " + EnumChatFormatting.RESET;
        MARK_HARD_SUFFIX = MARK_HARD_COLOR + " ]";
        MARK_SELECTED = MARK_SELECTED_PREFIX + MARK_SELECTED_SUFFIX;
        MARK_SOFT = MARK_SOFT_PREFIX + MARK_SOFT_SUFFIX;
        MARK_HARD = MARK_HARD_PREFIX + MARK_HARD_SUFFIX;
    }

    private static final int JUMP_HIGHLIGHT_COLOR = 0xD4A928;
    private static final int JUMP_HIGHLIGHT_MAX_ALPHA = 0x60;
    /** Pixels trimmed off the bar's top and bottom so it sits inside the key button's bevel. */
    private static final int CONTEXT_BAR_INSET = 1;
    /** Width of the context bar's hover target, which reaches past the bar so 2px is still easy to hit. */
    private static final int CONTEXT_BAR_HOVER_WIDTH = 6;
    private static final int GLYPH_SIZE = 9;
    private static final int KEYBOARD_ICON_SIZE = 14;
    /** Entry-relative left edge of the indicator row; the reset button ends at x + 274. */
    private static final int INDICATOR_LEFT = 275;
    private static final int INDICATOR_GAP = 1;
    /** Right edge of the indicator row, entry-relative; the row's content ends here, past the list width. */
    private static final int INDICATOR_RIGHT = INDICATOR_LEFT + GLYPH_SIZE * 3 + INDICATOR_GAP * 2;
    /** The key name is right-aligned to this entry-relative x, and clamped so it never runs off screen. */
    private static final int LABEL_RIGHT = 95;
    private static final int LABEL_CLAMP_LEFT = 4;
    /** Visual height of a glyph, one less than FONT_HEIGHT; vanilla centers text in a widget against this. */
    private static final int GLYPH_TEXT_HEIGHT = 8;

    private static final ResourceLocation ICON_LOCK = icon("lock_glyph");
    private static final ResourceLocation ICON_NO_MOUSE = icon("mouse_glyph");
    private static final ResourceLocation ICON_NO_KEYBOARD = icon("keyboard_glyph");
    private static final ResourceLocation ICON_KEYBOARD = icon("keyboard");

    private static ResourceLocation icon(String name) {
        return new ResourceLocation(Controlling.MODID, "textures/gui/icon/" + name + ".png");
    }

    /** Draws a square icon at its native size; the textures carry alpha, so blending stays on for the draw. */
    private static void drawIcon(ResourceLocation texture, int left, int top, int size) {
        final boolean wasBlending = GL11.glIsEnabled(GL11.GL_BLEND);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Gui.func_146110_a(left, top, 0.0F, 0.0F, size, size, size, size);
        if (!wasBlending) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private final GuiNewControls controlsScreen;
    private final Minecraft mc;
    private final List<IGuiListEntry> displayedEntries = new ArrayList<>();
    private final List<IGuiListEntry> allEntries = new ArrayList<>();
    private int maxListLabelWidth;
    private String hoveredKeyDescription;
    private String lowerSearch = "";
    private List<String> hoveredConflictLines;
    private String hoveredIndicatorText;
    private String hoveredComboText;

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
                    mcIn.fontRenderer.getStringWidth(StatCollector.translateToLocal(keybinding.getKeyDescription())));
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
        // Lowercased once per frame rather than twice per row.
        this.lowerSearch = this.controlsScreen.getSearchString().toLowerCase();
        refreshMarkers();
        this.hoveredKeyDescription = null;
        this.hoveredConflictLines = null;
        this.hoveredIndicatorText = null;
        this.hoveredComboText = null;
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

    public void drawHoveredComboTooltip(int mouseX, int mouseY) {
        if (this.hoveredComboText != null) {
            this.controlsScreen.drawKeyDescriptionTooltip(this.hoveredComboText, mouseX, mouseY);
        }
    }

    private static int contextColor(KeyContext context) {
        final ColorPalette p = ControllingConfig.palette;
        if (context == KeyContexts.IN_GAME) {
            return p.contextInGame;
        }
        if (context == KeyContexts.GUI) {
            return p.contextGui;
        }
        return p.contextCustom;
    }

    private static boolean isOver(int mouseX, int mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private static String contextDisplayName(KeyContext context) {
        final String key = context.translationKey();
        final String name = StatCollector.translateToLocal(key);
        // translateToLocal echoes the key when it is missing; fall back to the raw id for unlocalized custom contexts.
        return name.equals(key) ? context.id() : name;
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return this.displayedEntries.get(index);
    }

    @Override
    protected int getScrollBarX() {
        // Last term reserves the indicator row: three glyphs plus gaps, and the usual margin before the bar.
        return super.getScrollBarX() + 15 + 20 + 24;
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
        // Skip the lowercasing entirely when nothing is being searched for, which is the usual case.
        if (!highlight || this.lowerSearch.isEmpty()) {
            mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFF);
            return;
        }
        final int index = text.toLowerCase().indexOf(this.lowerSearch);
        if (index > -1) {
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
            this.labelText = StatCollector.translateToLocal(name);
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

        private String cachedName;
        private int cachedKeyCode = Integer.MIN_VALUE;
        private final IntArrayList cachedCombo = new IntArrayList();

        private final GuiButton btnChangeKeyBinding;
        private final GuiButton btnVisualKeyboard;
        private final GuiButton btnResetKeyBinding;

        private KeyEntry(final KeyBinding keyBinding, CategoryEntry categoryEntry) {
            this.keybinding = keyBinding;
            this.keyDesc = StatCollector.translateToLocal(keyBinding.getKeyDescription());
            this.btnChangeKeyBinding = new GuiButton(2000, 0, 0, 75 + 20, 20, this.keyDesc);
            this.btnVisualKeyboard = new GuiButton(2002, 0, 0, 18, 20, "");
            this.categoryEntry = categoryEntry;
            this.btnResetKeyBinding = new GuiButton(
                    2001,
                    0,
                    0,
                    50,
                    20,
                    StatCollector.translateToLocal("controls.reset"));
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, Tessellator tessellator,
                int mouseX, int mouseY, boolean isSelected) {
            this.drawJumpHighlight(x, y);
            boolean isKeySelected = controlsScreen.buttonId == this.keybinding;
            this.btnChangeKeyBinding.xPosition = x + 105;
            this.btnChangeKeyBinding.yPosition = y;
            // Align the name with the key button's own label: the slotHeight argument is the slot's content height,
            // which is shorter than the button, so centering on it puts the name a couple of pixels high.
            this.drawKeyDescription(x, y + (this.btnChangeKeyBinding.height - GLYPH_TEXT_HEIGHT) / 2, mouseX, mouseY);
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

            final String fullComboName = this.displayName();

            boolean hasConflict = false;
            boolean modConflict = true; // less severe form of conflict, like SHIFT conflicting with SHIFT+G
            // Only the hovered row builds a tooltip, so only it needs the list of conflicting bindings.
            final boolean overChangeButton = this.isMouseOverChangeButton(mouseX, mouseY);
            final List<KeyBinding> conflicts = overChangeButton ? new ArrayList<>() : null;

            if (this.keybinding.getKeyCode() != 0) {
                for (KeyBinding key : mc.gameSettings.keyBindings) {
                    if (key != this.keybinding) {
                        if (key instanceof ComboKeyBinding comboKeyBinding
                                && keybinding instanceof ComboKeyBinding comboKeyBinding2) {
                            if (comboKeyBinding.controlling$conflicts(keybinding)) {
                                hasConflict = true;
                                modConflict &= comboKeyBinding2.controlling$hasKeyCodeModifierConflict(key);
                                if (conflicts != null) {
                                    conflicts.add(key);
                                }
                            }
                        } else if (this.keybinding.getKeyCode() == key.getKeyCode()) {
                            hasConflict = true;
                            if (conflicts != null) {
                                conflicts.add(key);
                            }
                            break;
                        }
                    }
                }
            }

            if (hasConflict && conflicts != null && !conflicts.isEmpty()) {
                hoveredConflictLines = this.buildConflictTooltip(conflicts);
            }

            String prefix = "";
            String suffix = "";
            String markers = "";
            if (isKeySelected) {
                prefix = MARK_SELECTED_PREFIX;
                suffix = MARK_SELECTED_SUFFIX;
                markers = MARK_SELECTED;
            } else if (hasConflict) {
                prefix = modConflict ? MARK_SOFT_PREFIX : MARK_HARD_PREFIX;
                suffix = modConflict ? MARK_SOFT_SUFFIX : MARK_HARD_SUFFIX;
                markers = modConflict ? MARK_SOFT : MARK_HARD;
            }

            // Fit the combo label to the button: shrink the font first, then clip with an ellipsis as a last resort.
            final int available = this.btnChangeKeyBinding.width - 6;
            final int markerWidth = mc.fontRenderer.getStringWidth(markers);
            final int fullWidth = markerWidth + mc.fontRenderer.getStringWidth(fullComboName);
            String shownName = fullComboName;
            boolean truncated = false;
            float scale = 1.0F;
            if (fullWidth > available) {
                scale = (float) available / (float) fullWidth;
                if (scale < MIN_LABEL_SCALE) {
                    scale = MIN_LABEL_SCALE;
                    final int budget = (int) (available / MIN_LABEL_SCALE) - markerWidth
                            - mc.fontRenderer.getStringWidth("...");
                    shownName = mc.fontRenderer.trimStringToWidth(fullComboName, Math.max(0, budget)) + "...";
                    truncated = true;
                }
            }
            // Show the full combo on hover only when it still had to be clipped (conflict tooltip already lists names).
            if (truncated && overChangeButton && !hasConflict) {
                hoveredComboText = fullComboName;
            }

            final boolean searching = shouldHighlightKeyName();
            final int index = searching ? shownName.toLowerCase().indexOf(lowerSearch) : -1;
            final int indexEndHighlight = index + lowerSearch.length();
            final boolean highlight = searching && index > -1;
            String textStart = null;
            String textMiddle = null;
            String textEnd = null;
            if (highlight) {
                textStart = shownName.substring(0, index);
                textMiddle = shownName.substring(index, indexEndHighlight);
                textEnd = shownName.substring(indexEndHighlight);
            }

            final String buttonLabel = prefix + shownName + suffix;
            this.btnChangeKeyBinding.displayString = buttonLabel;
            this.drawScaledButtonLabel(
                    mouseX,
                    mouseY,
                    scale,
                    buttonLabel,
                    prefix,
                    shownName,
                    suffix,
                    highlight,
                    textStart,
                    textMiddle,
                    textEnd);

        }

        private void drawKeyboardIcon() {
            final int left = this.btnVisualKeyboard.xPosition + (this.btnVisualKeyboard.width - KEYBOARD_ICON_SIZE) / 2;
            final int top = this.btnVisualKeyboard.yPosition + (this.btnVisualKeyboard.height - KEYBOARD_ICON_SIZE) / 2;
            drawIcon(ICON_KEYBOARD, left, top, KEYBOARD_ICON_SIZE);
        }

        // Draws the button background, then the label centered and scaled down (scale < 1) so long combos fit.
        private void drawScaledButtonLabel(int mouseX, int mouseY, float scale, String buttonLabel, String prefix,
                String shownName, String suffix, boolean highlight, String textStart, String textMiddle,
                String textEnd) {
            final GuiButton btn = this.btnChangeKeyBinding;
            final String saved = btn.displayString;
            btn.displayString = "";
            btn.drawButton(mc, mouseX, mouseY);
            btn.displayString = saved;

            final int fontHeight = mc.fontRenderer.FONT_HEIGHT;
            final int totalWidth = mc.fontRenderer.getStringWidth(buttonLabel);
            final int left = Math.round(-totalWidth / 2.0F);
            final int top = -fontHeight / 2;

            GL11.glPushMatrix();
            GL11.glTranslatef(btn.xPosition + btn.width / 2.0F, btn.yPosition + btn.height / 2.0F, 0.0F);
            GL11.glScalef(scale, scale, 1.0F);

            if (highlight) {
                String middle = textMiddle;
                String end = textEnd;
                if (prefix.contains(EnumChatFormatting.UNDERLINE.toString())) {
                    middle = EnumChatFormatting.UNDERLINE + middle;
                    end = EnumChatFormatting.UNDERLINE + end;
                }
                final String drawnStart = prefix + textStart;
                final int rectLeft = left + mc.fontRenderer.getStringWidth(drawnStart);
                final int rectRight = rectLeft + mc.fontRenderer.getStringWidth(middle);
                Gui.drawRect(rectLeft, top, rectRight, top + fontHeight, YELLOW_HIGHLIGHT_COLOR);
                mc.fontRenderer.drawStringWithShadow(drawnStart, left, top, 0xFFFFFF);
                mc.fontRenderer.drawString(middle, rectLeft, top, DARK_TEXT_HIGHLIGHT_COLOR);
                mc.fontRenderer.drawStringWithShadow(end + suffix, rectRight, top, 0xFFFFFF);
            } else {
                final int color = this.isMouseOverChangeButton(mouseX, mouseY) ? 0xFFFFA0 : 0xE0E0E0;
                mc.fontRenderer.drawStringWithShadow(buttonLabel, left, top, color);
            }
            GL11.glPopMatrix();
        }

        /**
         * Fades a wash across the row after the visual keyboard jumps here. Bounds come from what the row actually
         * draws: the label runs left of x for long names, the indicators run past the list width, and the drawEntry
         * slotHeight argument is the slot's content height rather than the button height.
         */
        private void drawJumpHighlight(int x, int y) {
            final float strength = controlsScreen.getHighlightStrength(this.keybinding);
            if (strength <= 0.0F) {
                return;
            }
            final int alpha = (int) (strength * JUMP_HIGHLIGHT_MAX_ALPHA);
            Gui.drawRect(
                    this.labelLeftEdge(x) - 3,
                    y,
                    x + INDICATOR_RIGHT,
                    y + this.btnChangeKeyBinding.height,
                    (alpha << 24) | JUMP_HIGHLIGHT_COLOR);
        }

        /**
         * The bound-key label, recomputed only when the binding actually changes. Building it walks the combo and ends
         * in Keyboard.getKeyName, which was the most expensive thing this list did per row per frame.
         */
        private String displayName() {
            final int keyCode = this.keybinding.getKeyCode();
            if (this.keybinding instanceof ComboKeyBinding combo) {
                final IntList comboKeys = combo.controlling$comboKeysRaw();
                if (this.cachedName == null || keyCode != this.cachedKeyCode || !this.cachedCombo.equals(comboKeys)) {
                    this.cachedKeyCode = keyCode;
                    this.cachedCombo.clear();
                    this.cachedCombo.addAll(comboKeys);
                    this.cachedName = combo.controlling$getDisplayName();
                }
                return this.cachedName;
            }
            if (this.cachedName == null || keyCode != this.cachedKeyCode) {
                this.cachedKeyCode = keyCode;
                this.cachedName = GameSettings.getKeyDisplayString(keyCode);
            }
            return this.cachedName;
        }

        private boolean isMouseOverChangeButton(int mouseX, int mouseY) {
            final GuiButton btn = this.btnChangeKeyBinding;
            return mouseX >= btn.xPosition && mouseX < btn.xPosition + btn.width
                    && mouseY >= btn.yPosition
                    && mouseY < btn.yPosition + btn.height;
        }

        private List<String> buildConflictTooltip(List<KeyBinding> conflicts) {
            final List<String> lines = new ArrayList<>();
            lines.add(StatCollector.translateToLocal("options.conflictsHeader"));
            final ComboKeyBinding self = this.keybinding instanceof ComboKeyBinding combo ? combo : null;
            for (KeyBinding conflict : conflicts) {
                final boolean modifierConflict = self != null && self.controlling$hasKeyCodeModifierConflict(conflict);
                final String color = modifierConflict ? MARK_SOFT_COLOR : MARK_HARD_COLOR;
                final String name = StatCollector.translateToLocal(conflict.getKeyDescription());
                final String keyDisplay = conflict instanceof ComboKeyBinding combo ? combo.controlling$getDisplayName()
                        : GameSettings.getKeyDisplayString(conflict.getKeyCode());
                lines.add(color + name + EnumChatFormatting.GRAY + "  [" + keyDisplay + "]");
            }
            return lines;
        }

        private void drawIndicators(int x, int y, int mouseX, int mouseY) {
            final ComboKeyBinding combo = this.keybinding instanceof ComboKeyBinding c ? c : null;
            final KeyContext context = combo != null ? combo.controlling$getKeyContext() : KeyContexts.UNIVERSAL;
            final boolean lockedCombos = combo != null && !combo.controlling$allowsCombos();
            final boolean blockedComboKeys = combo != null && !combo.controlling$blockedComboKeys().isEmpty();
            final boolean noMouse = combo != null && !combo.controlling$allowsMouse();
            final boolean noKeyboard = combo != null && !combo.controlling$allowsKeyboard();

            // Context reads as a colored spine on the key button. The input-type flags sit in one row in the widened
            // gap after the reset button (reset ends at x + 274, scroll bar at x + 307), each in a fixed slot so a
            // given indicator is always in the same column.
            final int slotLock = x + INDICATOR_LEFT;
            final int slotMouse = slotLock + GLYPH_SIZE + INDICATOR_GAP;
            final int slotKeyboard = slotMouse + GLYPH_SIZE + INDICATOR_GAP;
            final int slotTop = y + (this.btnChangeKeyBinding.height - GLYPH_SIZE) / 2;

            if (context != KeyContexts.UNIVERSAL) {
                this.drawContextBar(contextColor(context));
                // Hover keeps the button's full height and reaches left into the gap; the bar itself is only 2px.
                if (isOver(
                        mouseX,
                        mouseY,
                        this.btnChangeKeyBinding.xPosition - CONTEXT_BAR_HOVER_WIDTH,
                        this.btnChangeKeyBinding.yPosition,
                        CONTEXT_BAR_HOVER_WIDTH,
                        this.btnChangeKeyBinding.height)) {
                    hoveredIndicatorText = StatCollector
                            .translateToLocalFormatted("options.contextLabel", contextDisplayName(context));
                }
            }
            // The lock covers both restrictions: combos barred outright, or only certain keys barred. The tooltip
            // says which, and is only built for the row under the cursor.
            if (lockedCombos || blockedComboKeys) {
                this.drawLockIcon(slotLock, slotTop);
                if (isOver(mouseX, mouseY, slotLock, slotTop, GLYPH_SIZE, GLYPH_SIZE)) {
                    hoveredIndicatorText = lockedCombos ? StatCollector.translateToLocal("options.combosLocked")
                            : StatCollector.translateToLocalFormatted(
                                    "options.combosBlocked",
                                    KeyNames.joinSorted(combo.controlling$blockedComboKeys(), ", "));
                }
            }
            if (noMouse) {
                this.drawMouseGlyph(slotMouse, slotTop);
                this.setIndicatorHover(mouseX, mouseY, slotMouse, slotTop, "options.mouseDisabled");
            }
            if (noKeyboard) {
                this.drawKeyboardGlyph(slotKeyboard, slotTop);
                this.setIndicatorHover(mouseX, mouseY, slotKeyboard, slotTop, "options.keyboardDisabled");
            }
        }

        /** Takes a lang key, not a string: this runs for every indicator on every visible row, every frame. */
        private void setIndicatorHover(int mouseX, int mouseY, int left, int top, String langKey) {
            if (isOver(mouseX, mouseY, left, top, GLYPH_SIZE, GLYPH_SIZE)) {
                hoveredIndicatorText = StatCollector.translateToLocal(langKey);
            }
        }

        /**
         * Bar butted against the left edge of the key button. Inset top and bottom because the button's bevel reads as
         * shadow, so a flat bar spanning the full height looks taller than the button next to it.
         */
        private void drawContextBar(int color) {
            final GuiButton btn = this.btnChangeKeyBinding;
            final int right = btn.xPosition;
            Gui.drawRect(
                    right - CONTEXT_BAR_WIDTH,
                    btn.yPosition + CONTEXT_BAR_INSET,
                    right,
                    btn.yPosition + btn.height - CONTEXT_BAR_INSET,
                    color);
        }

        private void drawLockIcon(int left, int top) {
            drawIcon(ICON_LOCK, left, top, GLYPH_SIZE);
        }

        private void drawMouseGlyph(int left, int top) {
            drawIcon(ICON_NO_MOUSE, left, top, GLYPH_SIZE);
        }

        private void drawKeyboardGlyph(int left, int top) {
            drawIcon(ICON_NO_KEYBOARD, left, top, GLYPH_SIZE);
        }

        /** The label is right-aligned to LABEL_RIGHT, so its left edge depends on how long the name is. */
        private int labelLeftEdge(int x) {
            final int labelRight = x + LABEL_RIGHT;
            final String label = trimWithEllipsis(this.keyDesc, Math.max(0, labelRight - LABEL_CLAMP_LEFT));
            return labelRight - mc.fontRenderer.getStringWidth(label);
        }

        private void drawKeyDescription(int x, int y, int mouseX, int mouseY) {
            final int labelLeft = LABEL_CLAMP_LEFT;
            final int labelRight = x + LABEL_RIGHT;
            final int maxWidth = Math.max(0, labelRight - labelLeft);
            final boolean truncated = mc.fontRenderer.getStringWidth(this.keyDesc) > maxWidth;
            final String label = trimWithEllipsis(this.keyDesc, maxWidth);
            final int labelX = this.labelLeftEdge(x);
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
