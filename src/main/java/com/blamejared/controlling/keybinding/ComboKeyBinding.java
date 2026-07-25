package com.blamejared.controlling.keybinding;

import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.api.KeyContext;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

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

    /**
     * Live poll: true when the main key and every combo key are physically down right now. Reads the keyboard and mouse
     * directly, so it is valid anywhere on the client - in game, inside any GUI, and between ticks. Ignores conflict
     * context and more specific bindings; see {@link #controlling$isChordActive()} for the firing decision.
     */
    boolean controlling$isChordDown();

    /**
     * Live poll: true when {@link #controlling$isChordDown()} holds, this binding's {@link KeyContext} is active, and
     * no more specific binding (one whose key set strictly contains this one's) is also held. This is the "would it
     * fire right now" question, so Ctrl+G being held does not report a bare G binding as active.
     */
    boolean controlling$isChordActive();

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

    /** Whether the user may attach any chord keys to this binding at all. */
    boolean controlling$allowsChords();

    void controlling$setAllowsChords(boolean allowsChords);

    /** Keys the user may not put in this binding's chord; never null, empty when unrestricted. */
    IntSet controlling$blockedChordKeys();

    void controlling$setBlockedChordKeys(IntList keys);

    /** Array counterpart, so the api package never has to name a fastutil type. */
    void controlling$setBlockedChordKeys(int[] keys);

    boolean controlling$allowsMouse();

    void controlling$setAllowsMouse(boolean allowsMouse);

    boolean controlling$allowsKeyboard();

    void controlling$setAllowsKeyboard(boolean allowsKeyboard);
}
