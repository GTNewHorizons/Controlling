package com.blamejared.controlling.keybinding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

class ComboStateTest {

    private static IntList list(int... values) {
        return new IntArrayList(values);
    }

    // down predicate over an explicit set of "held" keycodes
    private static IntPredicate held(int... down) {
        IntList set = list(down);
        return set::contains;
    }

    @Test
    void singleMainKeyDown_satisfied() {
        assertTrue(ComboState.satisfied(20, list(), held(20)));
    }

    @Test
    void singleMainKeyUp_notSatisfied() {
        assertFalse(ComboState.satisfied(20, list(), held(21)));
    }

    @Test
    void mainPlusComboAllDown_satisfied() {
        assertTrue(ComboState.satisfied(20, list(29), held(20, 29)));
    }

    @Test
    void mainDownComboUp_notSatisfied() {
        assertFalse(ComboState.satisfied(20, list(29), held(20)));
    }

    @Test
    void mouseComboKeyDown_satisfied() {
        // main = -100 (LMB), combo = LCONTROL(29)
        assertTrue(ComboState.satisfied(-100, list(29), held(-100, 29)));
    }

    @Test
    void noMainKeyButComboHeld_satisfied() {
        // main == KEY_NONE, combo non-empty and all down
        assertTrue(ComboState.satisfied(ComboState.KEY_NONE, list(29, 42), held(29, 42)));
    }

    @Test
    void noMainKeyEmptyCombo_notSatisfied() {
        assertFalse(ComboState.satisfied(ComboState.KEY_NONE, list(), held(29)));
    }

    @Test
    void strictSuperset_moreKeysIsSuperset() {
        // {LMB, Ctrl, Shift} strictly supersets {LMB, Ctrl}
        assertTrue(ComboState.isStrictSuperset(-100, list(29, 42), -100, list(29)));
    }

    @Test
    void strictSuperset_equalSetsNotSuperset() {
        assertFalse(ComboState.isStrictSuperset(20, list(29), 20, list(29)));
    }

    @Test
    void strictSuperset_disjointNotSuperset() {
        assertFalse(ComboState.isStrictSuperset(20, list(29), 21, list()));
    }

    @Test
    void sameKeySet_orderInsensitive() {
        assertTrue(ComboState.sameKeySet(20, list(29, 42), 20, list(42, 29)));
    }

    @Test
    void resolvedByPrecedence_subsetEitherDirection() {
        // bare G vs Ctrl+G: most-specific-wins picks one, so this is not a conflict
        assertTrue(ComboState.resolvedByPrecedence(20, list(), 20, list(29)));
        assertTrue(ComboState.resolvedByPrecedence(20, list(29), 20, list()));
    }

    @Test
    void resolvedByPrecedence_equalSetsAreNot() {
        // identical chords always fire together; precedence cannot separate them
        assertFalse(ComboState.resolvedByPrecedence(20, list(29), 20, list(29)));
    }

    @Test
    void resolvedByPrecedence_incomparableSetsAreNot() {
        // Ctrl+G vs Shift+G: holding Ctrl+Shift+G satisfies both, neither suppresses the other
        assertFalse(ComboState.resolvedByPrecedence(20, list(29), 20, list(42)));
    }

    @Test
    void sameKeySet_differentComboKeys() {
        assertFalse(ComboState.sameKeySet(20, list(29), 20, list(42)));
    }
}
