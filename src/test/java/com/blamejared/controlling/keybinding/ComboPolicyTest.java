package com.blamejared.controlling.keybinding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

class ComboPolicyTest {

    private static final int LCONTROL = 29;
    private static final int LSHIFT = 42;
    private static final int G = 34;

    private static IntSet blocked(int... keys) {
        return new IntOpenHashSet(keys);
    }

    private static int[] filtered(IntList held, boolean allowsCombos, IntSet blockedKeys) {
        final IntList out = new IntArrayList();
        ComboPolicy.filter(held, allowsCombos, blockedKeys, out);
        return out.toIntArray();
    }

    @Test
    void combosDisallowed_dropsEveryHeldKey() {
        // regression: capturing held keys in the controls list ignored the lock entirely
        final IntList held = new IntArrayList(new int[] { LCONTROL, LSHIFT, G });
        assertArrayEquals(new int[0], filtered(held, false, null));
    }

    @Test
    void combosAllowed_keepsUnblockedKeys() {
        final IntList held = new IntArrayList(new int[] { LCONTROL, LSHIFT });
        assertArrayEquals(new int[] { LCONTROL, LSHIFT }, filtered(held, true, null));
    }

    @Test
    void blockedKeysAreDroppedButOthersKept() {
        final IntList held = new IntArrayList(new int[] { LCONTROL, LSHIFT, G });
        assertArrayEquals(new int[] { LSHIFT, G }, filtered(held, true, blocked(LCONTROL)));
    }

    @Test
    void blockingEverythingHeldYieldsEmpty() {
        final IntList held = new IntArrayList(new int[] { LCONTROL, LSHIFT });
        assertArrayEquals(new int[0], filtered(held, true, blocked(LCONTROL, LSHIFT)));
    }

    @Test
    void accepts_mirrorsFilter() {
        assertFalse(ComboPolicy.accepts(LCONTROL, false, null));
        assertTrue(ComboPolicy.accepts(LCONTROL, true, null));
        assertFalse(ComboPolicy.accepts(LCONTROL, true, blocked(LCONTROL)));
        assertTrue(ComboPolicy.accepts(LSHIFT, true, blocked(LCONTROL)));
    }

    @Test
    void filterClearsPriorContents() {
        final IntList out = new IntArrayList(new int[] { 99 });
        ComboPolicy.filter(new IntArrayList(new int[] { LSHIFT }), true, null, out);
        assertArrayEquals(new int[] { LSHIFT }, out.toIntArray());
    }
}
