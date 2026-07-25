package com.blamejared.controlling.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.blamejared.controlling.Controlling;
import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.keybinding.ChordPolicy;
import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.KeyModifier;

import cpw.mods.fml.client.config.GuiCheckBox;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

@SideOnly(Side.CLIENT)
public class GuiNewControls extends GuiControls {

    private static final GameSettings.Options[] OPTIONS_ARR = new GameSettings.Options[] {
            GameSettings.Options.INVERT_MOUSE, GameSettings.Options.SENSITIVITY, GameSettings.Options.TOUCHSCREEN };

    private static final int DONE_BUTTON_ID = 1001;
    private static final int RESET_ALL_KEYS_BUTTON_ID = 1002;
    private static final int SHOW_UNBOUD_BUTTON_ID = 1003;
    private static final int SHOW_CONFLICTS_BUTTON_ID = 1004;
    private static final int SEARCH_KEYNAME_BUTTON_ID = 1005;
    private static final int SEARCH_CATEGORYNAME_BUTTON_ID = 1006;
    private static final int VISUAL_KEYBOARD_BUTTON_ID = 1007;
    private static final int SORT_TYPE_BUTTON_ID = 1008;

    private static final long HIGHLIGHT_DURATION_MS = 2500L;
    /** The flash holds full strength until this much time is left, then fades out. */
    private static final float HIGHLIGHT_FADE_MS = 700.0F;

    private final GuiScreen parentScreen;
    private final GameSettings options;
    private final String guiScreenTitle;
    private GuiNewKeyBindingList guiNewKeyBindingList;
    private GuiButton buttonReset;
    private String lastSearch = "";
    private GuiTextField searchTextBox;

    private DisplayMode displayMode = DisplayMode.ALL;
    private SearchType searchType = SearchType.ALL;
    private SortOrder sortOrder = SortOrder.VANILLA;

    private GuiButton buttonNone;
    private GuiButton buttonConflicting;
    private GuiCheckBox buttonKey;
    private GuiCheckBox buttonCat;
    private boolean confirmingReset = false;
    private final GuiVisualKeyboard visualKeyboard = new GuiVisualKeyboard();
    private boolean showVisualKeyboard = false;
    private final IntArrayList visualKeyboardChord = new IntArrayList();
    private KeyBinding highlightedBinding;
    private long highlightExpiry;
    /** Scratch buffer for {@link #getVisualKeyboardChord()}; avoids allocating every frame. */
    private final IntArrayList effectiveChord = new IntArrayList();
    private KeyModifier selectedModifier = KeyModifier.NONE;
    private int selectedModifierKeyCode = Keyboard.KEY_NONE;
    private KeyBinding pendingBinding;
    private KeyModifier pendingModifier = KeyModifier.NONE;
    private int pendingKeyCode = Keyboard.KEY_NONE;
    private List<Integer> pendingComboKeys = new ArrayList<>();

    public GuiNewControls(GuiScreen screen, GameSettings settings) {
        super(screen, settings);
        this.parentScreen = screen;
        this.options = settings;
        this.guiScreenTitle = StatCollector.translateToLocal("controls.title");
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        int i = 0;

        for (GameSettings.Options gameOption : OPTIONS_ARR) {
            if (gameOption.getEnumFloat()) {
                this.buttonList.add(
                        new GuiOptionSlider(
                                gameOption.returnEnumOrdinal(),
                                this.width / 2 - 155 + i % 2 * 160,
                                18 + 24 * (i >> 1),
                                gameOption));
            } else {
                this.buttonList.add(
                        new GuiOptionButton(
                                gameOption.returnEnumOrdinal(),
                                this.width / 2 - 155 + i % 2 * 160,
                                18 + 24 * (i >> 1),
                                gameOption,
                                this.options.getKeyBinding(gameOption)));
            }
            ++i;
        }

        this.guiNewKeyBindingList = new GuiNewKeyBindingList(this, this.mc);

        this.buttonList.add(
                new GuiButton(
                        DONE_BUTTON_ID,
                        this.width / 2 - 155 + 160,
                        this.height - 29,
                        150,
                        20,
                        StatCollector.translateToLocal("gui.done")));

        this.buttonReset = new GuiButton(
                RESET_ALL_KEYS_BUTTON_ID,
                this.width / 2 - 155,
                this.height - 29,
                150,
                20,
                StatCollector.translateToLocal("controls.resetAll"));
        this.buttonList.add(this.buttonReset);

        this.buttonNone = new GuiButton(
                SHOW_UNBOUD_BUTTON_ID,
                this.width / 2 - 155 + 160 + 76,
                this.height - 29 - 24,
                150 / 2,
                20,
                StatCollector.translateToLocal("options.showNone"));
        this.buttonList.add(this.buttonNone);

        this.buttonConflicting = new GuiButton(
                SHOW_CONFLICTS_BUTTON_ID,
                this.width / 2 - 155 + 160,
                this.height - 29 - 24,
                150 / 2,
                20,
                StatCollector.translateToLocal("options.showConflicts"));
        this.buttonList.add(this.buttonConflicting);

        this.searchTextBox = new GuiTextField(fontRendererObj, this.width / 2 - 154, this.height - 29 - 23, 148, 18);
        searchTextBox.setCanLoseFocus(true);

        this.buttonKey = new GuiCheckBox(
                SEARCH_KEYNAME_BUTTON_ID,
                this.width / 2 - (155 / 2),
                this.height - 29 - 37,
                StatCollector.translateToLocal("options.key"),
                false);
        this.buttonList.add(this.buttonKey);

        this.buttonCat = new GuiCheckBox(
                SEARCH_CATEGORYNAME_BUTTON_ID,
                this.width / 2 - (155 / 2),
                this.height - 29 - 50,
                StatCollector.translateToLocal("options.category"),
                false);
        this.buttonList.add(this.buttonCat);

        this.buttonList.add(
                new GuiButton(
                        VISUAL_KEYBOARD_BUTTON_ID,
                        this.width / 2 - 155 + 160,
                        this.height - 29 - 24 - 24,
                        150 / 2,
                        20,
                        StatCollector.translateToLocal("options.keyMap")));

        this.buttonList.add(
                new GuiButton(
                        SORT_TYPE_BUTTON_ID,
                        this.width / 2 - 155 + 160 + 76,
                        this.height - 29 - 24 - 24,
                        150 / 2,
                        20,
                        StatCollector.translateToLocal("options.sort") + ": " + sortOrder.getNextName()));
    }

    @Override
    public void updateScreen() {
        this.captureModifierOnlyBinding();
        this.applyPendingBindingIfReleased();
        this.searchTextBox.updateCursorCounter();
        if (!this.lastSearch.equals(this.searchTextBox.getText())) {
            this.filterKeys();
            this.lastSearch = this.searchTextBox.getText();
        }
    }

    private void filterKeys() {
        this.guiNewKeyBindingList.scrollBy(-this.guiNewKeyBindingList.getAmountScrolled());
        final Predicate<GuiNewKeyBindingList.KeyEntry> keyFilter = displayMode.getPredicate()
                .and(searchType.getPredicate(searchTextBox.getText()));
        final List<GuiNewKeyBindingList.KeyEntry> keysToDisplay = new ArrayList<>();
        for (GuiListExtended.IGuiListEntry entry : guiNewKeyBindingList.getAllEntries()) {
            if (entry instanceof GuiNewKeyBindingList.KeyEntry keyEntry) {
                if (keyFilter.test(keyEntry)) {
                    keysToDisplay.add(keyEntry);
                }
            }
        }
        sortOrder.sort(keysToDisplay);
        final List<GuiNewKeyBindingList.IGuiListEntry> entriesToDisplay = new ArrayList<>();
        GuiNewKeyBindingList.CategoryEntry prevCategory = null;
        for (GuiNewKeyBindingList.KeyEntry key : keysToDisplay) {
            if (key.getCategoryEntry() != prevCategory) {
                prevCategory = key.getCategoryEntry();
                entriesToDisplay.add(key.getCategoryEntry());
            }
            entriesToDisplay.add(key);
        }
        this.guiNewKeyBindingList.setDisplayedEntries(entriesToDisplay);
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.guiNewKeyBindingList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRendererObj, this.guiScreenTitle, this.width / 2, 8, 0xFFFFFF);
        boolean isModified = false;

        for (KeyBinding keybinding : this.options.keyBindings) {
            if (keybinding instanceof ComboKeyBinding comboKeyBinding) {
                if (!comboKeyBinding.controlling$isSetToDefaultValue()) {
                    isModified = true;
                    break;
                }
            } else {
                if (keybinding.getKeyCode() != keybinding.getKeyCodeDefault()) {
                    isModified = true;
                    break;
                }
            }
        }

        searchTextBox.drawTextBox();
        this.buttonReset.enabled = isModified;

        if (!isModified) {
            confirmingReset = false;
            buttonReset.displayString = StatCollector.translateToLocal("controls.resetAll");
        }

        for (GuiButton guiButton : this.buttonList) {
            guiButton.drawButton(mc, mouseX, mouseY);
        }

        String text = StatCollector.translateToLocal("options.search");
        drawCenteredString(
                fontRendererObj,
                text,
                this.width / 2 - (155 / 2) - (fontRendererObj.getStringWidth(text)) - 5,
                this.height - 29 - 42,
                0xFFFFFF);

        if (this.showVisualKeyboard) {
            this.visualKeyboard.draw(this, this.mc, mouseX, mouseY);
        } else {
            this.guiNewKeyBindingList.drawHoveredKeyDescriptionTooltip(mouseX, mouseY);
            this.guiNewKeyBindingList.drawHoveredConflictTooltip(mouseX, mouseY);
            this.guiNewKeyBindingList.drawHoveredIndicatorTooltip(mouseX, mouseY);
            this.guiNewKeyBindingList.drawHoveredChordTooltip(mouseX, mouseY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id < 100 && button instanceof GuiOptionButton) {
            this.options.setOptionValue(((GuiOptionButton) button).returnEnumOptions(), 1);
            button.displayString = this.options.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
        } else if (button.id == DONE_BUTTON_ID) {
            mc.displayGuiScreen(this.parentScreen);
        } else if (button.id == RESET_ALL_KEYS_BUTTON_ID) {
            if (!confirmingReset) {
                confirmingReset = true;
                button.displayString = StatCollector.translateToLocal("options.confirmReset");
                return;
            }

            confirmingReset = false;
            button.displayString = StatCollector.translateToLocal("controls.resetAll");

            for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
                if (keyBinding instanceof ComboKeyBinding comboKeyBinding) {
                    comboKeyBinding.controlling$setToDefault();
                } else {
                    keyBinding.setKeyCode(keyBinding.getKeyCodeDefault());
                }
            }
            this.options.saveOptions();
            KeyBinding.resetKeyBindingArrayAndHash();
        } else if (button.id == SHOW_UNBOUD_BUTTON_ID) {
            if (displayMode == DisplayMode.UNBOUND) {
                buttonNone.displayString = StatCollector.translateToLocal("options.showNone");
                displayMode = DisplayMode.ALL;
            } else {
                displayMode = DisplayMode.UNBOUND;
                buttonNone.displayString = StatCollector.translateToLocal("options.showAll");
                buttonConflicting.displayString = StatCollector.translateToLocal("options.showConflicts");
            }
            filterKeys();
        } else if (button.id == SHOW_CONFLICTS_BUTTON_ID) {
            if (displayMode == DisplayMode.CONFLICTING) {
                buttonConflicting.displayString = StatCollector.translateToLocal("options.showConflicts");
                displayMode = DisplayMode.ALL;
            } else {
                displayMode = DisplayMode.CONFLICTING;
                buttonConflicting.displayString = StatCollector.translateToLocal("options.showAll");
                buttonNone.displayString = StatCollector.translateToLocal("options.showNone");
            }
            filterKeys();
        } else if (button.id == SEARCH_KEYNAME_BUTTON_ID) {
            buttonCat.setIsChecked(false);
            searchType = buttonKey.isChecked() ? SearchType.KEY_NAME : SearchType.ALL;
            filterKeys();
        } else if (button.id == SEARCH_CATEGORYNAME_BUTTON_ID) {
            buttonKey.setIsChecked(false);
            searchType = buttonCat.isChecked() ? SearchType.CATEGORY_NAME : SearchType.ALL;
            filterKeys();
        } else if (button.id == VISUAL_KEYBOARD_BUTTON_ID) {
            this.showVisualKeyboard = !this.showVisualKeyboard;
            this.buttonId = null;
            this.visualKeyboardChord.clear();
        } else if (button.id == SORT_TYPE_BUTTON_ID) {
            sortOrder = sortOrder.getNext();
            button.displayString = StatCollector.translateToLocal("options.sort") + ": " + sortOrder.getNextName();
            filterKeys();
        }
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) {
        if (this.showVisualKeyboard && this.visualKeyboard.mouseClicked(this, mx, my, mb)) {
            searchTextBox.setFocused(false);
            return;
        } else if (this.buttonId != null) {
            if (this.buttonId instanceof ComboKeyBinding comboKeyBinding) {
                // Only bind a mouse button when mouse binds are allowed; otherwise ignore and keep the selection open.
                if (comboKeyBinding.controlling$allowsMouse()) {
                    this.schedulePendingBinding(this.buttonId, -100 + mb, this.getSelectedModifierForBinding());
                    this.selectedModifier = KeyModifier.NONE;
                    this.selectedModifierKeyCode = Keyboard.KEY_NONE;
                    KeyBinding.resetKeyBindingArrayAndHash();
                }
            } else {
                this.options.setOptionKeyBinding(this.buttonId, -100 + mb);
                this.buttonId = null;
                this.showVisualKeyboard = false;
                this.field_152177_g = Minecraft.getSystemTime();
                this.selectedModifier = KeyModifier.NONE;
                this.selectedModifierKeyCode = Keyboard.KEY_NONE;
                KeyBinding.resetKeyBindingArrayAndHash();
            }
            searchTextBox.setFocused(false);
        } else if (mb == 0 && !this.guiNewKeyBindingList.func_148179_a(mx, my, mb)) {
            // func_148179_a is mouseClicked but still obfuscated in 1.7.10
            try {
                superSuperMouseClicked(mx, my, mb);
            } catch (IOException e) {
                Controlling.LOGGER.error("Failed to forward mouse click to keybinding list", e);
            }
        }

        searchTextBox.mouseClicked(mx, my, mb);
        if (searchTextBox.isFocused() && mb == 1) {
            searchTextBox.setText("");
        }
    }

    protected void superSuperMouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            for (int i = 0; i < this.buttonList.size(); ++i) {
                GuiButton guibutton = this.buttonList.get(i);

                if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                    net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre event = new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre(
                            this,
                            guibutton,
                            this.buttonList);

                    if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) break;

                    guibutton = event.button;
                    this.selectedButton = guibutton;
                    guibutton.func_146113_a(this.mc.getSoundHandler());
                    this.actionPerformed(guibutton);

                    if (this.equals(this.mc.currentScreen)) {
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                                new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Post(
                                        this,
                                        event.button,
                                        this.buttonList));
                    }
                }
            }
        }
    }

    @Override
    public void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (state != 0 || !this.guiNewKeyBindingList.func_148181_b(mouseX, mouseY, state)) {
            // func_148181_b is mouseReleased but still obfuscated in 1.7.10
            superSuperMouseReleased(mouseX, mouseY, state);
        }
    }

    protected void superSuperMouseReleased(int mouseX, int mouseY, int state) {
        if (this.selectedButton != null && state == 0) {
            this.selectedButton.mouseReleased(mouseX, mouseY);
            this.selectedButton = null;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.buttonId != null) {
            final ComboKeyBinding comboKeyBinding = this.buttonId instanceof ComboKeyBinding
                    ? (ComboKeyBinding) this.buttonId
                    : null;
            final boolean isModifierKey = KeyModifier.isKeyCodeModifier(keyCode);
            boolean shouldCloseBindSelection = true;
            final int inputKeyCode = keyCode != Keyboard.KEY_NONE ? keyCode
                    : typedChar > 0 ? typedChar + 256 : Keyboard.KEY_NONE;

            if (keyCode == Keyboard.KEY_ESCAPE) {
                if (comboKeyBinding != null) {
                    comboKeyBinding.controlling$setKeyModifierAndCode(KeyModifier.NONE, Keyboard.KEY_NONE);
                }
                this.options.setOptionKeyBinding(this.buttonId, Keyboard.KEY_NONE);
                this.pendingBinding = null;
                this.pendingModifier = KeyModifier.NONE;
                this.pendingKeyCode = Keyboard.KEY_NONE;
                this.pendingComboKeys = new ArrayList<>();
                this.selectedModifier = KeyModifier.NONE;
                this.selectedModifierKeyCode = Keyboard.KEY_NONE;
            } else if (isModifierKey && comboKeyBinding != null && this.acceptsChordKey(comboKeyBinding, keyCode)) {
                this.selectedModifier = KeyModifier.fromKeyCode(keyCode);
                this.selectedModifierKeyCode = keyCode;
                shouldCloseBindSelection = false;
            } else if (inputKeyCode != Keyboard.KEY_NONE) {
                if (comboKeyBinding != null) {
                    // Keep the bind selection open when keyboard binds are disallowed so a mouse button can still be
                    // used; only actually bind when the keyboard key is allowed.
                    if (comboKeyBinding.controlling$allowsKeyboard()) {
                        this.schedulePendingBinding(this.buttonId, inputKeyCode, this.getSelectedModifierForBinding());
                        this.selectedModifier = KeyModifier.NONE;
                        this.selectedModifierKeyCode = Keyboard.KEY_NONE;
                    }
                    shouldCloseBindSelection = false;
                } else {
                    this.options.setOptionKeyBinding(this.buttonId, inputKeyCode);
                }
            }

            if (shouldCloseBindSelection) {
                this.buttonId = null;
                this.showVisualKeyboard = false;
            }

            this.field_152177_g = Minecraft.getSystemTime();
            KeyBinding.resetKeyBindingArrayAndHash();
        } else {
            if (this.showVisualKeyboard && keyCode == Keyboard.KEY_ESCAPE) {
                this.closeVisualKeyboard();
                return;
            }
            if (this.searchTextBox.isFocused()) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    this.searchTextBox.setFocused(false);
                } else {
                    this.searchTextBox.textboxKeyTyped(typedChar, keyCode);
                }
            } else {
                this.superSuperKeyTyped(typedChar, keyCode);
            }
        }
    }

    private void schedulePendingBinding(KeyBinding keyBinding, int keyCode, KeyModifier keyModifier) {
        this.pendingBinding = keyBinding;
        this.pendingKeyCode = keyCode;
        this.pendingModifier = keyModifier == null ? KeyModifier.NONE : keyModifier;
        this.pendingComboKeys = this.captureHeldComboKeys(keyCode);
    }

    // Collect every currently-held key/mouse button except the main key, to become the combo (chord) keys. Keys the
    // binding refuses are dropped here: this is the path that actually creates chords, so the check has to be here.
    private List<Integer> captureHeldComboKeys(int mainKeyCode) {
        final List<Integer> combo = new ArrayList<>();
        final ComboKeyBinding bind = this.buttonId instanceof ComboKeyBinding c ? c : null;
        for (int key = 1; key < Keyboard.getKeyCount(); key++) {
            if (key == mainKeyCode) {
                continue;
            }
            if (Keyboard.isKeyDown(key) && this.acceptsChordKey(bind, key)) {
                combo.add(key);
            }
        }
        for (int button = 0; button < Mouse.getButtonCount(); button++) {
            final int keyCode = ControllingApi.mouseButtonToKeyCode(button);
            if (keyCode != mainKeyCode && Mouse.isButtonDown(button) && this.acceptsChordKey(bind, keyCode)) {
                combo.add(keyCode);
            }
        }
        return combo;
    }

    void selectKeyBinding(KeyBinding keyBinding, boolean showVisualKeyboard) {
        this.buttonId = keyBinding;
        this.showVisualKeyboard = showVisualKeyboard;
        // Seed the chord from the binding so an existing combo can be edited rather than rebuilt.
        this.visualKeyboardChord.clear();
        if (showVisualKeyboard && keyBinding instanceof ComboKeyBinding comboKeyBinding) {
            this.visualKeyboardChord.addAll(comboKeyBinding.controlling$comboKeysRaw());
        }
    }

    void selectVisualKeyboardKey(int keyCode) {
        if (this.buttonId == null) {
            return;
        }
        if (this.buttonId instanceof ComboKeyBinding cb
                && !(ControllingApi.isMouseKeyCode(keyCode) ? cb.controlling$allowsMouse()
                        : cb.controlling$allowsKeyboard())) {
            return; // this input type is disallowed for this binding
        }

        if (this.buttonId instanceof ComboKeyBinding comboKeyBinding) {
            final IntList chord = this.getVisualKeyboardChord();
            final int mainIndex = chord.indexOf(keyCode);
            if (mainIndex >= 0) {
                chord.removeInt(mainIndex); // the main key is not also a chord key
            }
            comboKeyBinding.controlling$setComboKeysRaw(chord);
        }
        this.visualKeyboardChord.clear();
        this.options.setOptionKeyBinding(this.buttonId, keyCode);
        this.pendingBinding = null;
        this.pendingModifier = KeyModifier.NONE;
        this.pendingKeyCode = Keyboard.KEY_NONE;
        this.pendingComboKeys = new ArrayList<>();
        this.selectedModifier = KeyModifier.NONE;
        this.selectedModifierKeyCode = Keyboard.KEY_NONE;
        this.buttonId = null;
        this.showVisualKeyboard = false;
        this.field_152177_g = Minecraft.getSystemTime();
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    private void captureModifierOnlyBinding() {
        // The visual keyboard builds chords by toggling keys, so a modifier press there is never a binding on its own.
        if (this.showVisualKeyboard) {
            return;
        }
        if (!(this.buttonId instanceof ComboKeyBinding) || this.pendingBinding != null
                || this.selectedModifier == KeyModifier.NONE
                || this.selectedModifierKeyCode == Keyboard.KEY_NONE) {
            return;
        }
        if (Keyboard.isKeyDown(this.selectedModifierKeyCode)) {
            return;
        }
        this.schedulePendingBinding(this.buttonId, this.selectedModifierKeyCode, KeyModifier.NONE);
        this.selectedModifier = KeyModifier.NONE;
        this.selectedModifierKeyCode = Keyboard.KEY_NONE;
    }

    private KeyModifier getSelectedModifierForBinding() {
        if (this.showVisualKeyboard) {
            final IntList chord = this.getVisualKeyboardChord();
            return chord.size() == 1 ? KeyModifier.fromKeyCode(chord.getInt(0)) : KeyModifier.NONE;
        }
        // getActiveModifier reads physical keys, so it bypasses the checks done when a chord key is chosen; the
        // modifier has to be policed here too or it re-adds a blocked key behind the chord list's back.
        final KeyModifier modifier = this.selectedModifier == KeyModifier.NONE ? KeyModifier.getActiveModifier()
                : this.selectedModifier;
        final ComboKeyBinding bind = this.buttonId instanceof ComboKeyBinding c ? c : null;
        if (bind != null && !ChordPolicy
                .acceptsModifier(modifier, bind.controlling$allowsChords(), bind.controlling$blockedChordKeys())) {
            return KeyModifier.NONE;
        }
        return modifier;
    }

    private void applyPendingBindingIfReleased() {
        if (this.pendingBinding == null) {
            return;
        }
        if (this.isPendingInputStillActive()) {
            return;
        }

        if (this.pendingBinding instanceof ComboKeyBinding comboKeyBinding) {
            comboKeyBinding.controlling$setKeyModifierAndCode(this.pendingModifier, this.pendingKeyCode);
            if (!this.pendingComboKeys.isEmpty()) {
                comboKeyBinding.controlling$setComboKeys(this.pendingComboKeys);
            }
        }
        this.options.setOptionKeyBinding(this.pendingBinding, this.pendingKeyCode);
        this.pendingBinding = null;
        this.pendingModifier = KeyModifier.NONE;
        this.pendingKeyCode = Keyboard.KEY_NONE;
        this.pendingComboKeys = new ArrayList<>();
        this.buttonId = null;
        this.showVisualKeyboard = false;
        this.field_152177_g = Minecraft.getSystemTime();
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    private boolean isPendingInputStillActive() {
        if (this.pendingModifier != KeyModifier.NONE && this.pendingModifier.isActive()) {
            return true;
        }
        if (this.pendingKeyCode <= -100) {
            final int mouseButton = this.pendingKeyCode + 100;
            return mouseButton >= 0 && mouseButton < Mouse.getButtonCount() && Mouse.isButtonDown(mouseButton);
        }
        return this.pendingKeyCode > Keyboard.KEY_NONE && this.pendingKeyCode < 256
                && Keyboard.isKeyDown(this.pendingKeyCode);
    }

    protected void superSuperKeyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
        }
    }

    public SearchType getSearchType() {
        return this.searchType;
    }

    public String getSearchString() {
        return lastSearch;
    }

    KeyBinding getSelectedKeyBinding() {
        return this.buttonId;
    }

    /**
     * The chord the visual keyboard will attach to the next key picked: keys toggled in the panel plus any modifier
     * physically held right now, so both ways of building a chord work. Returns a shared buffer, valid until the next
     * call.
     */
    IntList getVisualKeyboardChord() {
        final ComboKeyBinding bind = this.buttonId instanceof ComboKeyBinding c ? c : null;
        this.effectiveChord.clear();
        if (bind != null && !bind.controlling$allowsChords()) {
            return this.effectiveChord;
        }
        for (int i = 0; i < this.visualKeyboardChord.size(); i++) {
            final int key = this.visualKeyboardChord.getInt(i);
            if (this.acceptsChordKey(bind, key)) {
                this.effectiveChord.add(key);
            }
        }
        for (KeyModifier modifier : KeyModifier.VALUES) {
            if (modifier == KeyModifier.NONE) {
                continue;
            }
            final int held = modifier.getHeldKeyCode();
            if (held != Keyboard.KEY_NONE && !this.effectiveChord.contains(held) && this.acceptsChordKey(bind, held)) {
                this.effectiveChord.add(held);
            }
        }
        return this.effectiveChord;
    }

    /** @return true when the binding under edit accepts this key into its chord. */
    private boolean acceptsChordKey(ComboKeyBinding bind, int keyCode) {
        if (bind == null) {
            return true;
        }
        return ChordPolicy.accepts(keyCode, bind.controlling$allowsChords(), bind.controlling$blockedChordKeys());
    }

    boolean acceptsChordKey(int keyCode) {
        return this.acceptsChordKey(this.buttonId instanceof ComboKeyBinding c ? c : null, keyCode);
    }

    /** Adds or removes a key from the chord being built in the visual keyboard. */
    void toggleVisualKeyboardChordKey(int keyCode) {
        if (!this.acceptsChordKey(keyCode)) {
            return;
        }
        final int index = this.visualKeyboardChord.indexOf(keyCode);
        if (index >= 0) {
            this.visualKeyboardChord.removeInt(index);
        } else {
            this.visualKeyboardChord.add(keyCode);
        }
    }

    boolean isVisualKeyboardChordKey(int keyCode) {
        return this.visualKeyboardChord.contains(keyCode);
    }

    void clearVisualKeyboardChord() {
        this.visualKeyboardChord.clear();
    }

    /** Flashes a row in the list so a jump from the visual keyboard lands somewhere visible. */
    private void highlightBinding(KeyBinding keyBinding) {
        this.highlightedBinding = keyBinding;
        this.highlightExpiry = Minecraft.getSystemTime() + HIGHLIGHT_DURATION_MS;
    }

    /** @return how far through the highlight flash this binding is, 0 when it is not highlighted. */
    float getHighlightStrength(KeyBinding keyBinding) {
        if (keyBinding != this.highlightedBinding) {
            return 0.0F;
        }
        final long remaining = this.highlightExpiry - Minecraft.getSystemTime();
        if (remaining <= 0) {
            this.highlightedBinding = null;
            return 0.0F;
        }
        return Math.min(1.0F, (float) remaining / HIGHLIGHT_FADE_MS);
    }

    /** Closes the panel leaving every binding untouched, unlike picking a key or pressing escape. */
    void closeVisualKeyboard() {
        this.showVisualKeyboard = false;
        this.buttonId = null;
        this.visualKeyboardChord.clear();
        this.pendingBinding = null;
        this.pendingModifier = KeyModifier.NONE;
        this.pendingKeyCode = Keyboard.KEY_NONE;
        this.pendingComboKeys = new ArrayList<>();
        this.selectedModifier = KeyModifier.NONE;
        this.selectedModifierKeyCode = Keyboard.KEY_NONE;
    }

    void drawVisualKeyboardTooltip(List<String> lines, int mouseX, int mouseY) {
        this.func_146283_a(lines, mouseX, mouseY);
    }

    void drawKeyDescriptionTooltip(String text, int mouseX, int mouseY) {
        this.func_146283_a(Collections.singletonList(text), mouseX, mouseY);
    }

    void drawConflictTooltip(List<String> lines, int mouseX, int mouseY) {
        this.func_146283_a(lines, mouseX, mouseY);
    }

    void showKeyBinding(KeyBinding keyBinding) {
        this.highlightBinding(keyBinding);
        if (this.guiNewKeyBindingList.scrollToKeyBinding(keyBinding)) {
            this.showVisualKeyboard = false;
            return;
        }

        this.searchTextBox.setText("");
        this.lastSearch = "";
        this.displayMode = DisplayMode.ALL;
        this.buttonNone.displayString = StatCollector.translateToLocal("options.showNone");
        this.buttonConflicting.displayString = StatCollector.translateToLocal("options.showConflicts");
        this.filterKeys();
        this.guiNewKeyBindingList.scrollToKeyBinding(keyBinding);
        this.showVisualKeyboard = false;
    }

}
