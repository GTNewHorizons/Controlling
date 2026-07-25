package com.blamejared.controlling.keybinding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

class ChordPolicyTest {

    private static final int LCONTROL = 29;
    private static final int LSHIFT = 42;
    private static final int G = 34;

    private static IntSet blocked(int... keys) {
        return new IntOpenHashSet(keys);
    }

    private static int[] filtered(IntList held, boolean allowsChords, IntSet blockedKeys) {
        final IntList out = new IntArrayList();
        ChordPolicy.filter(held, allowsChords, blockedKeys, out);
        return out.toIntArray();
    }

    @Test
    void chordsDisallowed_dropsEveryHeldKey() {
        // regression: capturing held keys in the controls list ignored the lock entirely
        final IntList held = new IntArrayList(new int[] { LCONTROL, LSHIFT, G });
        assertArrayEquals(new int[0], filtered(held, false, null));
    }

    @Test
    void chordsAllowed_keepsUnblockedKeys() {
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
        assertFalse(ChordPolicy.accepts(LCONTROL, false, null));
        assertTrue(ChordPolicy.accepts(LCONTROL, true, null));
        assertFalse(ChordPolicy.accepts(LCONTROL, true, blocked(LCONTROL)));
        assertTrue(ChordPolicy.accepts(LSHIFT, true, blocked(LCONTROL)));
    }

    @Test
    void blockedModifierIsRejected() {
        // regression: holding Ctrl set the modifier from live key state, re-adding a key the chord list had dropped
        assertFalse(ChordPolicy.acceptsModifier(KeyModifier.CONTROL, true, blocked(LCONTROL)));
        assertTrue(ChordPolicy.acceptsModifier(KeyModifier.SHIFT, true, blocked(LCONTROL)));
    }

    @Test
    void noModifierIsAlwaysAccepted() {
        assertTrue(ChordPolicy.acceptsModifier(KeyModifier.NONE, false, blocked(LCONTROL)));
        assertTrue(ChordPolicy.acceptsModifier(null, false, null));
    }

    @Test
    void chordsDisallowed_rejectsAnyModifier() {
        assertFalse(ChordPolicy.acceptsModifier(KeyModifier.CONTROL, false, null));
    }

    @Test
    void filterClearsPriorContents() {
        final IntList out = new IntArrayList(new int[] { 99 });
        ChordPolicy.filter(new IntArrayList(new int[] { LSHIFT }), true, null, out);
        assertArrayEquals(new int[] { LSHIFT }, out.toIntArray());
    }
}
