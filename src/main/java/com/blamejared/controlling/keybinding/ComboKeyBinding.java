package com.blamejared.controlling.keybinding;

import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.api.KeyContext;

import it.unimi.dsi.fastutil.ints.IntList;

public interface ComboKeyBinding {

    KeyModifier controlling$getKeyModifier();

    KeyModifier controlling$getDefaultKeyModifier();

    void controlling$setKeyModifier(KeyModifier keyModifier);

    void controlling$setDefaultKeyModifier(KeyModifier keyModifier);

    List<Integer> controlling$getComboKeys();

    void controlling$setComboKeys(List<Integer> keys);

    void controlling$setDefaultComboKeys(List<Integer> keys);

    /** Backing primitive list of combo keys; callers must treat it as read-only. */
    IntList controlling$comboKeysRaw();

    /** Primitive counterpart of {@link #controlling$setComboKeys(List)}; avoids boxing on the load path. */
    void controlling$setComboKeysRaw(IntList keys);

    /** The main key code (== KeyBinding.getKeyCode()); convenience for internal polling. */
    int controlling$mainKeyCode();

    int controlling$getComboHeldTicks();

    void controlling$setComboHeldTicks(int ticks);

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
