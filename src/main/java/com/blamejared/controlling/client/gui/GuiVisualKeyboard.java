package com.blamejared.controlling.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.KeyModifier;

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

    private Page page = Page.MAIN;
    private final List<KeyButton> keys = new ArrayList<>();
    private final List<RectButton> pageButtons = new ArrayList<>();
    private final List<ModifierButton> modifierButtons = new ArrayList<>();

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int keyboardLeft;
    private int keyboardTop;
    private int keyboardWidth;
    private int keyGap;
    private int keyHeight;
    private boolean qwertyLayout = true;

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

        for (RectButton pageButton : this.pageButtons) {
            pageButton.draw(mc, mouseX, mouseY, pageButton.page == this.page);
        }

        mc.fontRenderer.drawStringWithShadow(
                I18n.format("options.visualKeyboardModifier"),
                this.panelLeft + 8,
                this.panelTop + 38,
                MUTED_TEXT_COLOR);
        for (ModifierButton modifierButton : this.modifierButtons) {
            modifierButton.draw(mc, mouseX, mouseY, modifierButton.modifier == screen.getVisualKeyboardModifier());
        }

        for (KeyButton key : this.keys) {
            key.draw(screen, mc, mouseX, mouseY);
        }

        KeyModifier modifier = screen.getVisualKeyboardModifier();
        for (KeyButton key : this.keys) {
            if (key.contains(mouseX, mouseY)) {
                List<String> matchingBindings = key.getMatchingBindings(mc, modifier);
                if (!matchingBindings.isEmpty()) {
                    screen.drawVisualKeyboardTooltip(matchingBindings, mouseX, mouseY);
                }
                return;
            }
        }
    }

    public boolean mouseClicked(GuiNewControls screen, int mouseX, int mouseY, int mouseButton) {
        if (!this.isInPanel(mouseX, mouseY)) {
            return false;
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

        for (ModifierButton modifierButton : this.modifierButtons) {
            if (modifierButton.contains(mouseX, mouseY)) {
                screen.setVisualKeyboardModifier(modifierButton.modifier);
                return true;
            }
        }

        for (KeyButton key : this.keys) {
            if (key.contains(mouseX, mouseY) && key.enabled) {
                if (screen.getSelectedKeyBinding() == null) {
                    List<KeyBinding> matchingBindings = key
                            .getMatchingKeyBindings(Minecraft.getMinecraft(), screen.getVisualKeyboardModifier());
                    if (!matchingBindings.isEmpty()) {
                        screen.showKeyBinding(matchingBindings.get(0));
                    }
                    return true;
                }
                screen.selectVisualKeyboardKey(key.keyCode);
                return true;
            }
        }

        return true;
    }

    private boolean isInPanel(int mouseX, int mouseY) {
        return mouseX >= this.panelLeft && mouseX < this.panelRight
                && mouseY >= this.panelTop
                && mouseY < this.panelBottom;
    }

    private void layout(GuiNewControls screen) {
        this.qwertyLayout = screen.isQwertyLayout();
        int maxWidth = Math.max(220, screen.width - 24);
        this.keyboardWidth = Math.min(560, maxWidth - 16);
        int panelWidth = Math.min(screen.width - 8, this.keyboardWidth + 16);
        this.keyboardWidth = Math.max(180, panelWidth - 16);
        this.keyGap = this.keyboardWidth < 360 ? 2 : 4;
        this.keyHeight = Math.max(14, Math.min(24, (screen.height - 116) / 7));

        int rows = this.page == Page.MAIN ? 6 : 4;
        int headerHeight = 57;
        int footerHeight = 24;
        int keyboardHeight = rows * this.keyHeight + (rows - 1) * this.keyGap;
        int panelHeight = headerHeight + keyboardHeight + footerHeight;

        this.panelLeft = (screen.width - panelWidth) / 2;
        this.panelRight = this.panelLeft + panelWidth;
        this.panelTop = Math.max(18, (screen.height - panelHeight) / 2);
        this.panelBottom = this.panelTop + panelHeight;
        this.keyboardLeft = this.panelLeft + 8;
        this.keyboardTop = this.panelTop + headerHeight;

        this.layoutPageButtons();
        this.layoutModifierButtons();
        this.layoutKeys();
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

    private void layoutModifierButtons() {
        this.modifierButtons.clear();
        int labelTop = this.panelTop + 33;

        int buttonLeft = this.panelLeft + 82;
        int availableWidth = this.panelRight - buttonLeft - 8;
        int buttonWidth = Math.max(22, (availableWidth - 12) / 4);
        for (KeyModifier modifier : KeyModifier.VALUES) {
            this.modifierButtons.add(
                    new ModifierButton(modifier, buttonLeft, labelTop, buttonWidth, 18, modifier.getDisplayName()));
            buttonLeft += buttonWidth + 4;
        }
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
                key(Keyboard.KEY_Q, "Q", "A", 1.0D),
                key(Keyboard.KEY_W, "W", "Z", 1.0D),
                key(Keyboard.KEY_E, "E", 1.0D),
                key(Keyboard.KEY_R, "R", 1.0D),
                key(Keyboard.KEY_T, "T", 1.0D),
                key(Keyboard.KEY_Y, "Y", 1.0D),
                key(Keyboard.KEY_U, "U", 1.0D),
                key(Keyboard.KEY_I, "I", 1.0D),
                key(Keyboard.KEY_O, "O", 1.0D),
                key(Keyboard.KEY_P, "P", 1.0D),
                key(Keyboard.KEY_LBRACKET, "[", "^", 1.0D),
                key(Keyboard.KEY_RBRACKET, "]", "$", 1.0D),
                key(Keyboard.KEY_BACKSLASH, "\\", "*", 1.5D));

        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_CAPITAL, "Caps", 1.75D),
                key(Keyboard.KEY_A, "A", "Q", 1.0D),
                key(Keyboard.KEY_S, "S", 1.0D),
                key(Keyboard.KEY_D, "D", 1.0D),
                key(Keyboard.KEY_F, "F", 1.0D),
                key(Keyboard.KEY_G, "G", 1.0D),
                key(Keyboard.KEY_H, "H", 1.0D),
                key(Keyboard.KEY_J, "J", 1.0D),
                key(Keyboard.KEY_K, "K", 1.0D),
                key(Keyboard.KEY_L, "L", 1.0D),
                key(Keyboard.KEY_SEMICOLON, ";", "M", 1.0D),
                key(Keyboard.KEY_APOSTROPHE, "'", 1.0D),
                key(Keyboard.KEY_RETURN, "Enter", 2.25D));

        y += this.keyHeight + this.keyGap;
        this.addRow(
                y,
                unit,
                key(Keyboard.KEY_LSHIFT, "Shift", 2.25D),
                key(Keyboard.KEY_Z, "Z", "W", 1.0D),
                key(Keyboard.KEY_X, "X", 1.0D),
                key(Keyboard.KEY_C, "C", 1.0D),
                key(Keyboard.KEY_V, "V", 1.0D),
                key(Keyboard.KEY_B, "B", 1.0D),
                key(Keyboard.KEY_N, "N", 1.0D),
                key(Keyboard.KEY_M, "M", ",", 1.0D),
                key(Keyboard.KEY_COMMA, ",", ";", 1.0D),
                key(Keyboard.KEY_PERIOD, ".", ":", 1.0D),
                key(Keyboard.KEY_SLASH, "/", "!", 1.0D),
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
        return new KeyButton(keyCode, label, units);
    }

    private KeyButton key(int keyCode, String qwertyLabel, String azertyLabel, double units) {
        return new KeyButton(keyCode, this.qwertyLayout ? qwertyLabel : azertyLabel, units);
    }

    private void addRow(double y, double unitWidth, KeyButton... row) {
        double x = this.keyboardLeft;
        for (KeyButton key : row) {
            int width = Math.max(10, (int) Math.round(unitWidth * key.units + this.keyGap * (key.units - 1.0D)));
            key.setBounds((int) Math.round(x), (int) Math.round(y), width, this.keyHeight);
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

    private static class ModifierButton extends RectButton {

        private final KeyModifier modifier;

        private ModifierButton(KeyModifier modifier, int left, int top, int width, int height, String label) {
            super(null, left, top, width, height, label);
            this.modifier = modifier;
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

        private void draw(GuiNewControls screen, Minecraft mc, int mouseX, int mouseY) {
            KeyModifier modifier = screen.getVisualKeyboardModifier();
            this.enabled = !modifier.matches(this.keyCode);

            int bindings = this.getMatchingBindings(mc, modifier).size();
            int color = KEY_NORMAL_COLOR;
            if (!this.enabled) {
                color = KEY_DISABLED_COLOR;
            } else if (this.isSelected(screen, modifier)) {
                color = KEY_SELECTED_COLOR;
            } else if (bindings > 1) {
                color = KEY_CONFLICT_COLOR;
            } else if (bindings == 1) {
                color = KEY_BOUND_COLOR;
            } else if (this.contains(mouseX, mouseY)) {
                color = KEY_HOVER_COLOR;
            }

            Gui.drawRect(this.left, this.top, this.left + this.width, this.top + this.height, color);
            drawBorder(this.left, this.top, this.left + this.width, this.top + this.height, PANEL_BORDER_COLOR);
            drawCentered(
                    mc,
                    this.label,
                    this.left,
                    this.top,
                    this.width,
                    this.height,
                    this.enabled ? TEXT_COLOR : MUTED_TEXT_COLOR);
        }

        private boolean isSelected(GuiNewControls screen, KeyModifier modifier) {
            KeyBinding selected = screen.getSelectedKeyBinding();
            if (selected == null || selected.getKeyCode() != this.keyCode) {
                return false;
            }
            if (selected instanceof ComboKeyBinding comboKeyBinding) {
                return comboKeyBinding.controlling$getKeyModifier() == modifier;
            }
            return modifier == KeyModifier.NONE;
        }

        private List<String> getMatchingBindings(Minecraft mc, KeyModifier modifier) {
            List<String> bindings = new ArrayList<>();
            for (KeyBinding keyBinding : this.getMatchingKeyBindings(mc, modifier)) {
                bindings.add(I18n.format(keyBinding.getKeyDescription()));
            }
            return bindings;
        }

        private List<KeyBinding> getMatchingKeyBindings(Minecraft mc, KeyModifier modifier) {
            List<KeyBinding> bindings = new ArrayList<>();
            for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
                if (keyBinding.getKeyCode() != this.keyCode) {
                    continue;
                }
                KeyModifier bindingModifier = keyBinding instanceof ComboKeyBinding comboKeyBinding
                        ? comboKeyBinding.controlling$getKeyModifier()
                        : KeyModifier.NONE;
                if (bindingModifier == modifier) {
                    bindings.add(keyBinding);
                }
            }
            return bindings;
        }
    }

    private static void drawCentered(Minecraft mc, String text, int left, int top, int width, int height, int color) {
        int textX = left + width / 2 - mc.fontRenderer.getStringWidth(text) / 2;
        int textY = top + (height - mc.fontRenderer.FONT_HEIGHT) / 2;
        mc.fontRenderer.drawStringWithShadow(text, textX, textY, color);
    }
}
