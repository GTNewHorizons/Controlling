package com.blamejared.controlling.keybinding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import org.junit.jupiter.api.Test;

class ComboKeyCodecTest {

    @Test
    void formatEmptyList_emptyString() {
        assertEquals("", ComboKeyCodec.formatComboKeys(new IntArrayList()));
    }

    @Test
    void formatMultiple_csv() {
        assertEquals("29,-100", ComboKeyCodec.formatComboKeys(new IntArrayList(new int[] { 29, -100 })));
    }

    @Test
    void parseLegacyControl_isLegacy() {
        ComboKeyCodec.Parsed parsed = ComboKeyCodec.parse("CONTROL");
        assertTrue(parsed.legacy);
        assertEquals("CONTROL", parsed.legacyName);
    }

    @Test
    void parseCsv_intList() {
        ComboKeyCodec.Parsed parsed = ComboKeyCodec.parse("29,-100");
        assertFalse(parsed.legacy);
        assertArrayEquals(new int[] { 29, -100 }, parsed.comboKeys.toIntArray());
    }

    @Test
    void parseSingleInt_intList() {
        ComboKeyCodec.Parsed parsed = ComboKeyCodec.parse("42");
        assertFalse(parsed.legacy);
        assertArrayEquals(new int[] { 42 }, parsed.comboKeys.toIntArray());
    }

    @Test
    void roundTripCsv() {
        IntArrayList original = new IntArrayList(new int[] { 29, 42, -99 });
        ComboKeyCodec.Parsed parsed = ComboKeyCodec.parse(ComboKeyCodec.formatComboKeys(original));
        assertArrayEquals(original.toIntArray(), parsed.comboKeys.toIntArray());
    }
}
