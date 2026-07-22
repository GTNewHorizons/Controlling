package com.blamejared.controlling.keybinding;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.api.KeyContext;

public interface ComboKeyBinding {

    KeyModifier controlling$getKeyModifier();

    KeyModifier controlling$getDefaultKeyModifier();

    void controlling$setKeyModifier(KeyModifier keyModifier);

    void controlling$setDefaultKeyModifier(KeyModifier keyModifier);

    void controlling$setKeyModifierAndCode(KeyModifier keyModifier, int keyCode);

    String controlling$getDisplayName();

    boolean controlling$conflicts(KeyBinding other);

    boolean controlling$hasKeyCodeModifierConflict(KeyBinding other);

    boolean controlling$isSetToDefaultValue();

    void controlling$setToDefault();

    boolean controlling$isModifierActive();

    KeyContext controlling$getKeyContext();

    void controlling$setKeyContext(KeyContext keyContext);

    boolean controlling$allowsComboModifier();

    void controlling$setAllowsComboModifier(boolean allowsComboModifier);

    boolean controlling$allowsMouse();

    void controlling$setAllowsMouse(boolean allowsMouse);

    boolean controlling$allowsKeyboard();

    void controlling$setAllowsKeyboard(boolean allowsKeyboard);
}
