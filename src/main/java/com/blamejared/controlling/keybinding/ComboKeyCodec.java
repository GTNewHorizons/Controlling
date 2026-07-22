package com.blamejared.controlling.keybinding;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Pure format/parse for the third colon segment of a key_ options line. Writes combo keys as a CSV of keycodes; parse
 * stays tolerant of the legacy single-modifier-name format (CONTROL/SHIFT/ALT) written by older Controlling.
 */
public final class ComboKeyCodec {

    private ComboKeyCodec() {}

    /** Result of parsing the third segment: either a legacy modifier name or a list of combo keycodes. */
    public static final class Parsed {

        public final boolean legacy;
        public final String legacyName;
        public final IntList comboKeys;

        private Parsed(boolean legacy, String legacyName, IntList comboKeys) {
            this.legacy = legacy;
            this.legacyName = legacyName;
            this.comboKeys = comboKeys;
        }
    }

    /** CSV of keycodes; empty list yields an empty string. */
    public static String formatComboKeys(IntList keys) {
        if (keys.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(keys.get(i));
        }
        return sb.toString();
    }

    public static Parsed parse(String segment) {
        if ("CONTROL".equals(segment) || "SHIFT".equals(segment) || "ALT".equals(segment)) {
            return new Parsed(true, segment, new IntArrayList());
        }
        final IntList keys = new IntArrayList();
        if (segment != null && !segment.isEmpty()) {
            for (String part : segment.split(",")) {
                final String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    keys.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException ignored) {
                    // skip malformed entries; keeps loading resilient
                }
            }
        }
        return new Parsed(false, null, keys);
    }
}
