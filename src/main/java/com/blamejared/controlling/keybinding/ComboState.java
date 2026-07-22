package com.blamejared.controlling.keybinding;

import java.util.function.IntPredicate;

import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Pure combo-key logic: no Minecraft or LWJGL imports so it is unit-testable. Callers pass current key state via an
 * IntPredicate and combo keys as a fastutil primitive list.
 */
public final class ComboState {

    /** Unbound / no key, matching Keyboard.KEY_NONE. */
    public static final int KEY_NONE = 0;

    private ComboState() {}

    /**
     * A combo is satisfied when the main key is down (or main is KEY_NONE and there is at least one combo key) and every
     * combo key is down.
     */
    public static boolean satisfied(int mainKey, IntList comboKeys, IntPredicate isDown) {
        if (mainKey == KEY_NONE) {
            if (comboKeys.isEmpty()) {
                return false;
            }
        } else if (!isDown.test(mainKey)) {
            return false;
        }
        for (int i = 0; i < comboKeys.size(); i++) {
            if (!isDown.test(comboKeys.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** True when the key set of A strictly contains the key set of B (A has all of B's keys plus at least one more). */
    public static boolean isStrictSuperset(int mainA, IntList comboA, int mainB, IntList comboB) {
        int sizeA = keySetSize(mainA, comboA);
        int sizeB = keySetSize(mainB, comboB);
        if (sizeA <= sizeB) {
            return false;
        }
        return containsAll(mainA, comboA, mainB, comboB);
    }

    /** True when both binds have identical key sets (order-insensitive, main included unless KEY_NONE). */
    public static boolean sameKeySet(int mainA, IntList comboA, int mainB, IntList comboB) {
        if (keySetSize(mainA, comboA) != keySetSize(mainB, comboB)) {
            return false;
        }
        return containsAll(mainA, comboA, mainB, comboB);
    }

    // number of distinct keys in {main unless NONE} union combo keys
    private static int keySetSize(int mainKey, IntList comboKeys) {
        int count = mainKey == KEY_NONE ? 0 : 1;
        for (int i = 0; i < comboKeys.size(); i++) {
            int key = comboKeys.get(i);
            if (key == mainKey || firstIndexOf(comboKeys, key) != i) {
                continue; // duplicate or equal to main
            }
            count++;
        }
        return count;
    }

    // every key in set B is present in set A
    private static boolean containsAll(int mainA, IntList comboA, int mainB, IntList comboB) {
        if (mainB != KEY_NONE && !inSet(mainA, comboA, mainB)) {
            return false;
        }
        for (int i = 0; i < comboB.size(); i++) {
            if (!inSet(mainA, comboA, comboB.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean inSet(int mainKey, IntList comboKeys, int key) {
        return key == mainKey || comboKeys.contains(key);
    }

    private static int firstIndexOf(IntList list, int value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == value) {
                return i;
            }
        }
        return -1;
    }
}
