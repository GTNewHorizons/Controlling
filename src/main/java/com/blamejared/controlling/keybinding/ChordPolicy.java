package com.blamejared.controlling.keybinding;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Decides which keys a binding will accept into its chord. Pure so it can be unit tested, and shared by every path that
 * builds a chord: capturing held keys in the controls list, toggling keys in the visual keyboard, and reading back the
 * chord under construction.
 */
public final class ChordPolicy {

    private ChordPolicy() {}

    /** @return true when {@code key} may join the chord of a binding with these restrictions. */
    public static boolean accepts(int key, boolean allowsChords, IntCollection blocked) {
        if (!allowsChords) {
            return false;
        }
        return blocked == null || !blocked.contains(key);
    }

    /**
     * Replaces {@code out} with the keys of {@code held} the binding will accept. A binding that disallows chords
     * yields an empty result rather than a partial one.
     */
    public static void filter(IntList held, boolean allowsChords, IntCollection blocked, IntList out) {
        out.clear();
        if (!allowsChords) {
            return;
        }
        for (int i = 0; i < held.size(); i++) {
            final int key = held.getInt(i);
            if (blocked == null || !blocked.contains(key)) {
                out.add(key);
            }
        }
    }
}
