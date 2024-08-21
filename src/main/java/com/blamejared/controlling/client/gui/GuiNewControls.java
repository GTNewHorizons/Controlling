package com.blamejared.controlling.client.gui;

import java.io.IOException;
import java.util.ArrayList;
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

import com.blamejared.controlling.Controlling;
import committee.nova.mkb.api.IKeyBinding;
import committee.nova.mkb.keybinding.KeyModifier;

import cpw.mods.fml.client.config.GuiCheckBox;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiNewControls extends GuiControls {

    private static final GameSettings.Options[] OPTIONS_ARR = new GameSettings.Options[] {
            GameSettings.Options.INVERT_MOUSE, GameSettings.Options.SENSITIVITY, GameSettings.Options.TOUCHSCREEN };

    private static final int KEYBOARD_LAYOUT_BUTTON_ID = 999;
    private static final int DONE_BUTTON_ID = 1001;
    private static final int RESET_ALL_KEYS_BUTTON_ID = 1002;
    private static final int SHOW_UNBOUD_BUTTON_ID = 1003;
    private static final int SHOW_CONFLICTS_BUTTON_ID = 1004;
    private static final int SEARCH_KEYNAME_BUTTON_ID = 1005;
    private static final int SEARCH_CATEGORYNAME_BUTTON_ID = 1006;
    private static final int SORT_TYPE_BUTTON_ID = 1008;

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
    private boolean isQwertyLayout;

    public GuiNewControls(GuiScreen screen, GameSettings settings) {
        super(screen, settings);
        this.parentScreen = screen;
        this.options = settings;
        this.guiScreenTitle = StatCollector.translateToLocal("controls.title");
        this.isQwertyLayout = !(this.options.keyBindForward.getKeyCode() == Keyboard.KEY_Z);
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

        this.buttonList.add(
                new GuiButton(
                        KEYBOARD_LAYOUT_BUTTON_ID,
                        this.width / 2 - 155 + i % 2 * 160,
                        18 + 24 * (i >> 1),
                        150,
                        20,
                        StatCollector.translateToLocal("options.keyboardLayout")
                                + (this.isQwertyLayout ? "QWERTY" : "AZERTY")));

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
                        SORT_TYPE_BUTTON_ID,
                        this.width / 2 - 155 + 160 + 76,
                        this.height - 29 - 24 - 24,
                        150 / 2,
                        20,
                        StatCollector.translateToLocal("options.sort") + ": " + sortOrder.getNextName()));
    }

    @Override
    public void updateScreen() {
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
            if (entry instanceof GuiNewKeyBindingList.KeyEntry) {
                GuiNewKeyBindingList.KeyEntry keyEntry = (GuiNewKeyBindingList.KeyEntry) entry;
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
        boolean flag = false;

        for (KeyBinding keybinding : this.options.keyBindings) {
            if (Controlling.isModernKeybindingInstalled && keybinding instanceof IKeyBinding modernKB) {
                if (!modernKB.isSetToDefaultValue()) {
                    flag = true;
                    break;
                }
            } else {
                if (keybinding.getKeyCode() != keybinding.getKeyCodeDefault()) {
                    flag = true;
                    break;
                }
            }
        }

        searchTextBox.drawTextBox();
        this.buttonReset.enabled = flag;

        if (!flag) {
            confirmingReset = false;
            buttonReset.displayString = StatCollector.translateToLocal("controls.resetAll");
        }

        for (GuiButton guiButton : (List<GuiButton>) this.buttonList) {
            guiButton.drawButton(mc, mouseX, mouseY);
        }

        String text = StatCollector.translateToLocal("options.search");
        drawCenteredString(
                fontRendererObj,
                text,
                this.width / 2 - (155 / 2) - (fontRendererObj.getStringWidth(text)) - 5,
                this.height - 29 - 42,
                0xFFFFFF);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id < 100 && button instanceof GuiOptionButton) {
            this.options.setOptionValue(((GuiOptionButton) button).returnEnumOptions(), 1);
            button.displayString = this.options.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
        } else if (button.id == KEYBOARD_LAYOUT_BUTTON_ID) {
            this.isQwertyLayout = !this.isQwertyLayout;
            button.displayString = StatCollector.translateToLocal("options.keyboardLayout")
                    + (this.isQwertyLayout ? "QWERTY" : "AZERTY");
            bindKeysToDefaultKeyboardLayout();
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
                if (Controlling.isModernKeybindingInstalled && keyBinding instanceof IKeyBinding modernKB) {
                    modernKB.setToDefault();
                } else {
                    keyBinding.setKeyCode(keyBinding.getKeyCodeDefault());
                }
            }
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
        } else if (button.id == SORT_TYPE_BUTTON_ID) {
            sortOrder = sortOrder.getNext();
            button.displayString = StatCollector.translateToLocal("options.sort") + ": " + sortOrder.getNextName();
            filterKeys();
        }
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) {
        if (this.buttonId != null) {
            if (Controlling.isModernKeybindingInstalled && this.buttonId instanceof IKeyBinding modernKB) {
                modernKB.setKeyModifierAndCode(KeyModifier.getActiveModifier(), -100 + mb);
            }
            this.options.setOptionKeyBinding(this.buttonId, -100 + mb);
            this.buttonId = null;
            KeyBinding.resetKeyBindingArrayAndHash();
            searchTextBox.setFocused(false);
        } else if (mb == 0 && !this.guiNewKeyBindingList.func_148179_a(mx, my, mb)) {
            // func_148179_a is mouseClicked but still obfuscated in 1.7.10
            try {
                superSuperMouseClicked(mx, my, mb);
            } catch (IOException e) {
                e.printStackTrace();
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
                GuiButton guibutton = (GuiButton) this.buttonList.get(i);

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
            if (Controlling.isModernKeybindingInstalled && this.buttonId instanceof IKeyBinding modernKB) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    modernKB.setKeyModifierAndCode(KeyModifier.NONE, Keyboard.KEY_NONE);
                } else if (keyCode != Keyboard.KEY_NONE) {
                    modernKB.setKeyModifierAndCode(KeyModifier.getActiveModifier(), keyCode);
                } else if (typedChar > 0) {
                    modernKB.setKeyModifierAndCode(KeyModifier.getActiveModifier(), typedChar + 256);
                }
            }

            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.options.setOptionKeyBinding(this.buttonId, Keyboard.KEY_NONE);
            } else if (keyCode != Keyboard.KEY_NONE) {
                this.options.setOptionKeyBinding(this.buttonId, keyCode);
            } else if (typedChar > 0) {
                this.options.setOptionKeyBinding(this.buttonId, typedChar + 256);
            }

            // logic - if modern keybinding is not installed, or the key pressed was not a modifier
            if (!Controlling.isModernKeybindingInstalled || !KeyModifier.isKeyCodeModifier(keyCode)) {
                this.buttonId = null;
            }

            this.field_152177_g = Minecraft.getSystemTime();
            KeyBinding.resetKeyBindingArrayAndHash();
        } else {
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

    protected void superSuperKeyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
        }
    }

    /**
     * When the associated GuiButton is pressed, it will bind the vanilla minecraft keys to the default values for a
     * QWERTY keyboard. When pressed again it will bind them to the default values for an AZERTY keyboard
     * <p>
     * QWERTY : Go Left -> A Walk Forward -> W Drop Item -> Q
     * <p>
     * AZERTY : Go Left -> Q Walk Forward -> Z Drop Item -> A
     */
    private void bindKeysToDefaultKeyboardLayout() {
        if (this.isQwertyLayout) {
            this.options.keyBindLeft.setKeyCode(this.options.keyBindLeft.getKeyCodeDefault());
            this.options.keyBindForward.setKeyCode(this.options.keyBindForward.getKeyCodeDefault());
            this.options.keyBindDrop.setKeyCode(this.options.keyBindDrop.getKeyCodeDefault());
        } else {
            this.options.keyBindLeft.setKeyCode(Keyboard.KEY_Q);
            this.options.keyBindForward.setKeyCode(Keyboard.KEY_Z);
            this.options.keyBindDrop.setKeyCode(Keyboard.KEY_A);
        }
        this.options.saveOptions();
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    public SearchType getSearchType() {
        return this.searchType;
    }

    public String getSearchString() {
        return lastSearch;
    }

}
