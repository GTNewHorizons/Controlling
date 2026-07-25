/**
 * Portions of this file are adapted from Keyboard Wizard / KeyboardWizard-Legacy-Forge-1.7.10.<br/>
 * Original project authors listed as MrNerdy42 and Tapio; copyright (c) 2022 MikhailTapio.<br/>
 * Keyboard Wizard is licensed under the MIT License; see THIRD_PARTY_NOTICES.md.<br/>
 */
package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.ComboState;
import com.blamejared.controlling.keybinding.KeyNames;

import cpw.mods.fml.common.Loader;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

public class GuiVisualKeyboard {

    private static final int PANEL_COLOR = 0xEE101010;
    private static final int PANEL_BORDER_COLOR = 0xFF777777;
    private static final int KEY_NORMAL_COLOR = 0xFF555555;
    private static final int KEY_HOVER_COLOR = 0xFF888888;
    private static final int KEY_BOUND_COLOR = 0xFF236B23;
    private static final int KEY_CONFLICT_COLOR = 0xFF8A2525;
    private static final int KEY_SELECTED_COLOR = 0xFFD4A928;
    private static final int KEY_DISABLED_COLOR = 0xFF303030;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int MUTED_TEXT_COLOR = 0xA0A0A0;

    /** LMB, RMB, MMB and two extra buttons; matches what most mice report. */
    private static final int MOUSE_BUTTON_COUNT = 5;
    private static final int KEY_CHORD_COLOR = 0xFF2F5FA8;
    /** Legend baseline, measured up from the panel bottom; leaves a gap between the key rows and the swatches. */
    private static final int LEGEND_BOTTOM_OFFSET = 28;
    private static final int LEGEND_SWATCH = 7;
    private static final int LEGEND_SWATCH_GAP = 3;
    private static final int LEGEND_ITEM_GAP = 8;
    private static final int CHORD_BORDER_COLOR = 0xFFD4A928;

    private Page page = Page.MAIN;
    private final List<KeyButton> keys = new ArrayList<>();
    private final List<KeyButton> mouseKeys = new ArrayList<>();
    private final List<RectButton> pageButtons = new ArrayList<>();
    private RectButton clearButton;

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int keyboardLeft;
    private int keyboardTop;
    private int keyboardWidth;
    private int keyGap;
    private int keyHeight;

    public void draw(GuiNewControls screen, Minecraft mc, int mouseX, int mouseY) {
        this.layout(screen);

        Gui.drawRect(this.panelLeft, this.panelTop, this.panelRight, this.panelBottom, PANEL_COLOR);
        drawBorder(this.panelLeft, this.panelTop, this.panelRight, this.panelBottom, PANEL_BORDER_COLOR);

        String title = I18n.format("options.visualKeyboard");
        mc.fontRenderer.drawStringWithShadow(title, this.panelLeft + 8, this.panelTop + 7, TEXT_COLOR);

        if (screen.getSelectedKeyBinding() != null) {
            String hint = I18n.format("options.visualKeyboardHint");
            mc.fontRenderer.drawStringWithShadow(hint, this.panelLeft + 8, this.panelBottom - 14, MUTED_TEXT_COLOR);
        }

        this.drawLegend(mc);

        for (RectButton pageButton : this.pageButtons) {
            pageButton.draw(mc, mouseX, mouseY, pageButton.page == this.page);
        }

        this.drawChordRow(screen, mc, mouseX, mouseY);

        final IntList chord = screen.getVisualKeyboardChord();
        for (KeyButton key : this.keys) {
            key.draw(screen, mc, chord, mouseX, mouseY);
        }
        for (KeyButton key : this.mouseKeys) {
            key.draw(screen, mc, chord, mouseX, mouseY);
        }

        final KeyButton hovered = this.hit(mouseX, mouseY);
        if (hovered != null) {
            List<String> matchingBindings = hovered.getMatchingBindings(mc, screen.getVisualKeyboardChord());
            if (!matchingBindings.isEmpty()) {
                screen.drawVisualKeyboardTooltip(matchingBindings, mouseX, mouseY);
            }
        }
    }

    /**
     * Key to the button colors, along the top of the panel footer. Items are dropped from the right when the panel is
     * too narrow to hold them all, so the most important ones survive on small screens.
     */
    private void drawLegend(Minecraft mc) {
        final int[] colors = { KEY_BOUND_COLOR, KEY_CONFLICT_COLOR, KEY_CHORD_COLOR, KEY_SELECTED_COLOR,
                KEY_DISABLED_COLOR };
        final String[] labels = { I18n.format("options.legendBound"), I18n.format("options.legendConflict"),
                I18n.format("options.legendChord"), I18n.format("options.legendSelected"),
                I18n.format("options.legendDisabled") };

        final int available = this.panelRight - this.panelLeft - 16;
        int shown = labels.length;
        while (shown > 0 && legendWidth(mc, labels, shown) > available) {
            shown--;
        }

        int left = this.panelLeft + 8;
        final int top = this.panelBottom - LEGEND_BOTTOM_OFFSET;
        for (int i = 0; i < shown; i++) {
            Gui.drawRect(left, top + 1, left + LEGEND_SWATCH, top + 1 + LEGEND_SWATCH, colors[i]);
            drawBorder(left, top + 1, left + LEGEND_SWATCH, top + 1 + LEGEND_SWATCH, PANEL_BORDER_COLOR);
            left += LEGEND_SWATCH + LEGEND_SWATCH_GAP;
            mc.fontRenderer.drawStringWithShadow(labels[i], left, top, MUTED_TEXT_COLOR);
            left += mc.fontRenderer.getStringWidth(labels[i]) + LEGEND_ITEM_GAP;
        }
    }

    private static int legendWidth(Minecraft mc, String[] labels, int count) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += LEGEND_SWATCH + LEGEND_SWATCH_GAP + mc.fontRenderer.getStringWidth(labels[i]) + LEGEND_ITEM_GAP;
        }
        return total - LEGEND_ITEM_GAP;
    }

    // Header line: the chord being built, plus a Clear button once it is non-empty.
    private void drawChordRow(GuiNewControls screen, Minecraft mc, int mouseX, int mouseY) {
        if (!this.allowsModifiers(screen)) {
            mc.fontRenderer.drawStringWithShadow(
                    I18n.format("options.modifiersLocked"),
                    this.panelLeft + 8,
                    this.panelTop + 38,
                    MUTED_TEXT_COLOR);
            return;
        }
        final IntList chord = screen.getVisualKeyboardChord();
        final String label = chord.isEmpty() ? I18n.format("options.visualKeyboardChordEmpty")
                : I18n.format("options.visualKeyboardChord", KeyNames.joinChord(chord));
        mc.fontRenderer.drawStringWithShadow(label, this.panelLeft + 8, this.panelTop + 38, MUTED_TEXT_COLOR);
        if (this.clearButton != null && !chord.isEmpty()) {
            this.clearButton.draw(mc, mouseX, mouseY, false);
        }
    }

    /** The key button under the cursor across both the keyboard and the mouse row, or null. */
    private KeyButton hit(int mouseX, int mouseY) {
        for (KeyButton key : this.keys) {
            if (key.contains(mouseX, mouseY)) {
                return key;
            }
        }
        for (KeyButton key : this.mouseKeys) {
            if (key.contains(mouseX, mouseY)) {
                return key;
            }
        }
        return null;
    }

    public boolean mouseClicked(GuiNewControls screen, int mouseX, int mouseY, int mouseButton) {
        if (!this.isInPanel(mouseX, mouseY)) {
            return false;
        }

        // Right-click toggles a key in or out of the chord that will be attached to the next key picked.
        if (mouseButton == 1) {
            // Also works with nothing selected: the chord then filters which bindings the keys light up for.
            final KeyButton key = this.hit(mouseX, mouseY);
            if (key != null && this.allowsModifiers(screen)) {
                screen.toggleVisualKeyboardChordKey(key.keyCode);
            }
            return true;
        }

        if (mouseButton != 0) {
            return true;
        }

        for (RectButton pageButton : this.pageButtons) {
            if (pageButton.contains(mouseX, mouseY)) {
                this.page = pageButton.page;
                return true;
            }
        }

        if (this.clearButton != null && !screen.getVisualKeyboardChord().isEmpty()
                && this.clearButton.contains(mouseX, mouseY)) {
            screen.clearVisualKeyboardChord();
            return true;
        }

        final KeyButton key = this.hit(mouseX, mouseY);
        if (key != null && key.enabled) {
            if (screen.getSelectedKeyBinding() == null) {
                List<KeyBinding> matchingBindings = key
                        .getMatchingKeyBindings(Minecraft.getMinecraft(), screen.getVisualKeyboardChord());
                if (!matchingBindings.isEmpty()) {
                    screen.showKeyBinding(matchingBindings.get(0));
                }
                return true;
            }
            screen.selectVisualKeyboardKey(key.keyCode);
        }

        return true;
    }

    private boolean isInPanel(int mouseX, int mouseY) {
        return mouseX >= this.panelLeft && mouseX < this.panelRight
                && mouseY >= this.panelTop
                && mouseY < this.panelBottom;
    }

    private boolean allowsModifiers(GuiNewControls screen) {
        return !(screen.getSelectedKeyBinding() instanceof ComboKeyBinding comboKeyBinding)
                || comboKeyBinding.controlling$allowsComboModifier();
    }

    private void layout(GuiNewControls screen) {
        int maxWidth = Math.max(220, screen.width - 24);
        this.keyboardWidth = Math.min(560, maxWidth - 16);
        int panelWidth = Math.min(screen.width - 8, this.keyboardWidth + 16);
        this.keyboardWidth = Math.max(180, panelWidth - 16);
        this.keyGap = this.keyboardWidth < 360 ? 2 : 4;
        this.keyHeight = Math.max(14, Math.min(24, (screen.height - 116) / 7));

        int headerHeight = 57;
        // Tall enough to clear the keys before the legend starts; see LEGEND_BOTTOM_OFFSET.
        int footerHeight = 34;
        // keyboard rows, then the mouse row
        int bodyHeight = this.keyboardHeight() + this.keyGap + this.keyHeight;
        int panelHeight = headerHeight + bodyHeight + footerHeight;

        this.panelLeft = (screen.width - panelWidth) / 2;
        this.panelRight = this.panelLeft + panelWidth;
        this.panelTop = Math.max(18, (screen.height - panelHeight) / 2);
        this.panelBottom = this.panelTop + panelHeight;
        this.keyboardLeft = this.panelLeft + 8;
        this.keyboardTop = this.panelTop + headerHeight;

        this.layoutPageButtons();
        this.layoutChordRow();
        this.layoutKeys();
        this.layoutMouseKeys();
    }

    private void layoutPageButtons() {
        this.pageButtons.clear();
        int buttonTop = this.panelTop + 6;
        int buttonWidth = Math.max(42, Math.min(58, (this.panelRight - this.panelLeft - 24) / 3));
        int buttonLeft = this.panelRight - 8 - buttonWidth * 3 - 8;
        this.pageButtons.add(
                new RectButton(
                        Page.MAIN,
                        buttonLeft,
                        buttonTop,
                        buttonWidth,
                        18,
                        I18n.format("options.visualKeyboardMain")));
        this.pageButtons.add(
                new RectButton(
                        Page.NUMPAD,
                        buttonLeft + buttonWidth + 4,
                        buttonTop,
                        buttonWidth,
                        18,
                        I18n.format("options.visualKeyboardNumpad")));
        this.pageButtons.add(
                new RectButton(
                        Page.AUX,
                        buttonLeft + (buttonWidth + 4) * 2,
                        buttonTop,
                        buttonWidth,
                        18,
                        I18n.format("options.visualKeyboardAux")));
    }

    private void layoutChordRow() {
        final int buttonWidth = 40;
        this.clearButton = new RectButton(
                null,
                this.panelRight - 8 - buttonWidth,
                this.panelTop + 33,
                buttonWidth,
                18,
                I18n.format("options.visualKeyboardClear"));
    }

    // Mouse buttons live on their own row below the keyboard so mouse chords and mouse main keys are reachable here.
    private void layoutMouseKeys() {
        this.mouseKeys.clear();
        final int top = this.keyboardTop + this.keyboardHeight() + this.keyGap;
        final int buttonWidth = Math.max(24, Math.min(48, (this.keyboardWidth - this.keyGap * 4) / 5));
        int left = this.keyboardLeft;
        for (int button = 0; button < MOUSE_BUTTON_COUNT; button++) {
            final int keyCode = ControllingApi.mouseButtonToKeyCode(button);
            final KeyButton key = new KeyButton(keyCode, KeyNames.display(keyCode), 1.0D);
            key.setBounds(left, top, buttonWidth, this.keyHeight);
            this.mouseKeys.add(key);
            left += buttonWidth + this.keyGap;
        }
    }

    private int keyboardHeight() {
        final int rows = this.page == Page.MAIN ? 6 : 4;
        return rows * this.keyHeight + (rows - 1) * this.keyGap;
    }

    private void layoutKeys() {
        this.keys.clear();
        switch (this.page) {
            case MAIN:
                this.layoutMainKeys();
                return;
            case NUMPAD:
                this.layoutNumpadKeys();
                return;
            case AUX:
                this.layoutAuxKeys();
        }
    }

    private void layoutMainKeys() {
        double unit = (this.keyboardWidth - this.keyGap * 14) / 15.0D;
        double y = this.keyboardTop;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_ESCAPE, "Esc", 1.0D),
                key(Keyboard.KEY_F1, "F1", 1.0D),
                key(Keyboard.KEY_F2, "F2", 1.0D),
                key(Keyboard.KEY_F3, "F3", 1.0D),
                key(Keyboard.KEY_F4, "F4", 1.0D),
                key(Keyboard.KEY_F5, "F5", 1.0D),
                key(Keyboard.KEY_F6, "F6", 1.0D),
                key(Keyboard.KEY_F7, "F7", 1.0D),
                key(Keyboard.KEY_F8, "F8", 1.0D),
                key(Keyboard.KEY_F9, "F9", 1.0D),
                key(Keyboard.KEY_F10, "F10", 1.0D),
                key(Keyboard.KEY_F11, "F11", 1.0D),
                key(Keyboard.KEY_F12, "F12", 1.0D));

        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_GRAVE, "`", 1.0D),
                key(Keyboard.KEY_1, "1", 1.0D),
                key(Keyboard.KEY_2, "2", 1.0D),
                key(Keyboard.KEY_3, "3", 1.0D),
                key(Keyboard.KEY_4, "4", 1.0D),
                key(Keyboard.KEY_5, "5", 1.0D),
                key(Keyboard.KEY_6, "6", 1.0D),
                key(Keyboard.KEY_7, "7", 1.0D),
                key(Keyboard.KEY_8, "8", 1.0D),
                key(Keyboard.KEY_9, "9", 1.0D),
                key(Keyboard.KEY_0, "0", 1.0D),
                key(Keyboard.KEY_MINUS, "-", 1.0D),
                key(Keyboard.KEY_EQUALS, "=", 1.0D),
                key(Keyboard.KEY_BACK, "Back", 2.0D));

        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_TAB, "Tab", 1.5D),
                key(Keyboard.KEY_Q, "Q", 1.0D),
                key(Keyboard.KEY_W, "W", 1.0D),
                key(Keyboard.KEY_E, "E", 1.0D),
                key(Keyboard.KEY_R, "R", 1.0D),
                key(Keyboard.KEY_T, "T", 1.0D),
                key(Keyboard.KEY_Y, "Y", 1.0D),
                key(Keyboard.KEY_U, "U", 1.0D),
                key(Keyboard.KEY_I, "I", 1.0D),
                key(Keyboard.KEY_O, "O", 1.0D),
                key(Keyboard.KEY_P, "P", 1.0D),
                key(Keyboard.KEY_LBRACKET, "[", 1.0D),
                key(Keyboard.KEY_RBRACKET, "]", 1.0D),
                key(Keyboard.KEY_BACKSLASH, "\\", 1.5D));

        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_CAPITAL, "Caps", 1.75D),
                key(Keyboard.KEY_A, "A", 1.0D),
                key(Keyboard.KEY_S, "S", 1.0D),
                key(Keyboard.KEY_D, "D", 1.0D),
                key(Keyboard.KEY_F, "F", 1.0D),
                key(Keyboard.KEY_G, "G", 1.0D),
                key(Keyboard.KEY_H, "H", 1.0D),
                key(Keyboard.KEY_J, "J", 1.0D),
                key(Keyboard.KEY_K, "K", 1.0D),
                key(Keyboard.KEY_L, "L", 1.0D),
                key(Keyboard.KEY_SEMICOLON, ";", 1.0D),
                key(Keyboard.KEY_APOSTROPHE, "'", 1.0D),
                key(Keyboard.KEY_RETURN, "Enter", 2.25D));

        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_LSHIFT, "Shift", 2.25D),
                key(Keyboard.KEY_Z, "Z", 1.0D),
                key(Keyboard.KEY_X, "X", 1.0D),
                key(Keyboard.KEY_C, "C", 1.0D),
                key(Keyboard.KEY_V, "V", 1.0D),
                key(Keyboard.KEY_B, "B", 1.0D),
                key(Keyboard.KEY_N, "N", 1.0D),
                key(Keyboard.KEY_M, "M", 1.0D),
                key(Keyboard.KEY_COMMA, ",", 1.0D),
                key(Keyboard.KEY_PERIOD, ".", 1.0D),
                key(Keyboard.KEY_SLASH, "/", 1.0D),
                key(Keyboard.KEY_RSHIFT, "Shift", 2.75D));

        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_LCONTROL, "Ctrl", 1.25D),
                key(Keyboard.KEY_LMETA, "Win", 1.25D),
                key(Keyboard.KEY_LMENU, "Alt", 1.25D),
                key(Keyboard.KEY_SPACE, "Space", 7.5D),
                key(Keyboard.KEY_RMENU, "Alt", 1.25D),
                key(Keyboard.KEY_RMETA, "Win", 1.25D),
                key(Keyboard.KEY_RCONTROL, "Ctrl", 1.25D));
    }

    private void layoutNumpadKeys() {
        double unit = (this.keyboardWidth - this.keyGap * 3) / 4.0D;
        double y = this.keyboardTop;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_NUMLOCK, "Num", 1.0D),
                key(Keyboard.KEY_DIVIDE, "/", 1.0D),
                key(Keyboard.KEY_MULTIPLY, "*", 1.0D),
                key(Keyboard.KEY_SUBTRACT, "-", 1.0D));
        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_NUMPAD7, "7", 1.0D),
                key(Keyboard.KEY_NUMPAD8, "8", 1.0D),
                key(Keyboard.KEY_NUMPAD9, "9", 1.0D),
                key(Keyboard.KEY_ADD, "+", 1.0D));
        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_NUMPAD4, "4", 1.0D),
                key(Keyboard.KEY_NUMPAD5, "5", 1.0D),
                key(Keyboard.KEY_NUMPAD6, "6", 1.0D),
                key(Keyboard.KEY_NUMPADENTER, "Enter", 1.0D));
        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_NUMPAD1, "1", 1.0D),
                key(Keyboard.KEY_NUMPAD2, "2", 1.0D),
                key(Keyboard.KEY_NUMPAD3, "3", 1.0D),
                key(Keyboard.KEY_DECIMAL, ".", 1.0D));
    }

    private void layoutAuxKeys() {
        double unit = (this.keyboardWidth - this.keyGap * 4) / 5.0D;
        double y = this.keyboardTop;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_SYSRQ, "PrtSc", 1.0D),
                key(Keyboard.KEY_SCROLL, "ScrLk", 1.0D),
                key(Keyboard.KEY_PAUSE, "Pause", 1.0D),
                key(Keyboard.KEY_INSERT, "Ins", 1.0D),
                key(Keyboard.KEY_HOME, "Home", 1.0D));
        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_PRIOR, "PgUp", 1.0D),
                key(Keyboard.KEY_DELETE, "Del", 1.0D),
                key(Keyboard.KEY_END, "End", 1.0D),
                key(Keyboard.KEY_NEXT, "PgDn", 1.0D),
                key(Keyboard.KEY_UP, "Up", 1.0D));
        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_LEFT, "Left", 1.0D),
                key(Keyboard.KEY_DOWN, "Down", 1.0D),
                key(Keyboard.KEY_RIGHT, "Right", 1.0D),
                key(Keyboard.KEY_F13, "F13", 1.0D),
                key(Keyboard.KEY_F14, "F14", 1.0D));
        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_F15, "F15", 1.0D),
                key(Keyboard.KEY_F16, "F16", 1.0D),
                key(Keyboard.KEY_F17, "F17", 1.0D),
                key(Keyboard.KEY_F18, "F18", 1.0D),
                key(Keyboard.KEY_F19, "F19", 1.0D));
    }

    private KeyButton key(int keyCode, String label, double units) {
        return new KeyButton(keyCode, this.getKeyLabel(keyCode, label), units);
    }

    private String getKeyLabel(int keyCode, String fallback) {
        if (!Loader.isModLoaded("lwjgl3ify") || this.shouldUseFixedLabel(keyCode)) {
            return fallback;
        }
        String keyName = Keyboard.getKeyName(keyCode);
        return keyName == null || keyName.startsWith("Key ") ? fallback : keyName;
    }

    private boolean shouldUseFixedLabel(int keyCode) {
        return switch (keyCode) {
            // spotless:off
            case Keyboard.KEY_ESCAPE, Keyboard.KEY_BACK, Keyboard.KEY_TAB, Keyboard.KEY_CAPITAL,
                    Keyboard.KEY_RETURN, Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT, Keyboard.KEY_LCONTROL,
                    Keyboard.KEY_RCONTROL, Keyboard.KEY_LMENU, Keyboard.KEY_RMENU, Keyboard.KEY_LMETA,
                    Keyboard.KEY_RMETA, Keyboard.KEY_SPACE, Keyboard.KEY_NUMLOCK, Keyboard.KEY_SYSRQ,
                    Keyboard.KEY_SCROLL, Keyboard.KEY_INSERT, Keyboard.KEY_DELETE, Keyboard.KEY_HOME,
                    Keyboard.KEY_END, Keyboard.KEY_PRIOR, Keyboard.KEY_NEXT, Keyboard.KEY_UP, Keyboard.KEY_DOWN,
                    Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT, Keyboard.KEY_NUMPADENTER -> true;
            default -> false;
            // spotless:on
        };
    }

    /**
     * Advances in exact units and rounds each key's left and right edge, rather than rounding widths and summing them.
     * Rounding widths lets a fractional unit round up once per key, so a full row overshoots and eats the right gutter;
     * rounding edges keeps the error under half a pixel and lands the last key on keyboardLeft + keyboardWidth.
     */
    private void addRow(double y, double unitWidth, KeyButton... row) {
        double x = this.keyboardLeft;
        final int top = (int) Math.round(y);
        for (KeyButton key : row) {
            final double width = unitWidth * key.units + this.keyGap * (key.units - 1.0D);
            final int left = (int) Math.round(x);
            final int right = (int) Math.round(x + width);
            key.setBounds(left, top, Math.max(10, right - left), this.keyHeight);
            this.keys.add(key);
            x += width + this.keyGap;
        }
    }

    private static void drawBorder(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top, left + 1, bottom, color);
        Gui.drawRect(right - 1, top, right, bottom, color);
    }

    private enum Page {
        MAIN,
        NUMPAD,
        AUX
    }

    private static class RectButton {

        private final Page page;
        protected final int left;
        protected final int top;
        protected final int width;
        protected final int height;
        private final String label;

        private RectButton(Page page, int left, int top, int width, int height, String label) {
            this.page = page;
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.label = label;
        }

        protected boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.left && mouseX < this.left + this.width
                    && mouseY >= this.top
                    && mouseY < this.top + this.height;
        }

        protected void draw(Minecraft mc, int mouseX, int mouseY, boolean active) {
            int color = active ? KEY_SELECTED_COLOR
                    : this.contains(mouseX, mouseY) ? KEY_HOVER_COLOR : KEY_NORMAL_COLOR;
            Gui.drawRect(this.left, this.top, this.left + this.width, this.top + this.height, color);
            drawBorder(this.left, this.top, this.left + this.width, this.top + this.height, PANEL_BORDER_COLOR);
            drawCentered(mc, this.label, this.left, this.top, this.width, this.height, TEXT_COLOR);
        }
    }

    private static class KeyButton {

        private final int keyCode;
        private final String label;
        private final double units;
        private boolean enabled = true;
        private int left;
        private int top;
        private int width;
        private int height;

        private KeyButton(int keyCode, String label, double units) {
            this.keyCode = keyCode;
            this.label = label;
            this.units = units;
        }

        private void setBounds(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.left && mouseX < this.left + this.width
                    && mouseY >= this.top
                    && mouseY < this.top + this.height;
        }

        private void draw(GuiNewControls screen, Minecraft mc, IntList chord, int mouseX, int mouseY) {
            final boolean inChord = screen.isVisualKeyboardChordKey(this.keyCode);
            // A chord member cannot also be the main key, so it is not selectable while toggled on.
            this.enabled = this.allowsInputType(screen) && !inChord;

            int bindings = this.countMatchingBindings(mc, chord);
            int color = KEY_NORMAL_COLOR;
            if (inChord) {
                color = KEY_CHORD_COLOR;
            } else if (!this.enabled) {
                color = KEY_DISABLED_COLOR;
            } else if (this.isSelected(screen, chord)) {
                color = KEY_SELECTED_COLOR;
            } else if (bindings > 1) {
                color = KEY_CONFLICT_COLOR;
            } else if (bindings == 1) {
                color = KEY_BOUND_COLOR;
            } else if (this.contains(mouseX, mouseY)) {
                color = KEY_HOVER_COLOR;
            }

            Gui.drawRect(this.left, this.top, this.left + this.width, this.top + this.height, color);
            drawBorder(
                    this.left,
                    this.top,
                    this.left + this.width,
                    this.top + this.height,
                    inChord ? CHORD_BORDER_COLOR : PANEL_BORDER_COLOR);
            drawCentered(
                    mc,
                    this.label,
                    this.left,
                    this.top,
                    this.width,
                    this.height,
                    this.enabled || inChord ? TEXT_COLOR : MUTED_TEXT_COLOR);
        }

        // Bindings may opt out of mouse or keyboard input; the mouse row and the keys honor the matching flag.
        private boolean allowsInputType(GuiNewControls screen) {
            if (!(screen.getSelectedKeyBinding() instanceof ComboKeyBinding comboKeyBinding)) {
                return true;
            }
            return ControllingApi.isMouseKeyCode(this.keyCode) ? comboKeyBinding.controlling$allowsMouse()
                    : comboKeyBinding.controlling$allowsKeyboard();
        }

        private boolean isSelected(GuiNewControls screen, IntList chord) {
            KeyBinding selected = screen.getSelectedKeyBinding();
            if (selected == null || selected.getKeyCode() != this.keyCode) {
                return false;
            }
            return this.chordMatches(selected, chord);
        }

        // Coloring only needs the count, and this runs for every key every frame; do not build a list for it.
        private int countMatchingBindings(Minecraft mc, IntList chord) {
            int count = 0;
            for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
                if (this.isMatch(keyBinding, chord)) {
                    count++;
                }
            }
            return count;
        }

        private List<String> getMatchingBindings(Minecraft mc, IntList chord) {
            List<String> bindings = new ArrayList<>();
            for (KeyBinding keyBinding : this.getMatchingKeyBindings(mc, chord)) {
                bindings.add(I18n.format(keyBinding.getKeyDescription()));
            }
            return bindings;
        }

        private List<KeyBinding> getMatchingKeyBindings(Minecraft mc, IntList chord) {
            List<KeyBinding> bindings = new ArrayList<>();
            for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
                if (this.isMatch(keyBinding, chord)) {
                    bindings.add(keyBinding);
                }
            }
            return bindings;
        }

        private boolean isMatch(KeyBinding keyBinding, IntList chord) {
            if (keyBinding.getKeyCode() != this.keyCode || keyBinding.getKeyCategory().endsWith(".hidden")) {
                return false;
            }
            return this.chordMatches(keyBinding, chord);
        }

        // A binding lights up under this key only when its whole chord equals the one being built.
        private boolean chordMatches(KeyBinding keyBinding, IntList chord) {
            final IntList bindingChord = keyBinding instanceof ComboKeyBinding comboKeyBinding
                    ? comboKeyBinding.controlling$comboKeysRaw()
                    : IntLists.EMPTY_LIST;
            return ComboState.sameKeySet(this.keyCode, chord, this.keyCode, bindingChord);
        }
    }

    private static void drawCentered(Minecraft mc, String text, int left, int top, int width, int height, int color) {
        int textX = left + width / 2 - mc.fontRenderer.getStringWidth(text) / 2;
        int textY = top + (height - mc.fontRenderer.FONT_HEIGHT) / 2;
        mc.fontRenderer.drawStringWithShadow(text, textX, textY, color);
    }
}
