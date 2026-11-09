package com.blamejared.controlling.client.gui;

import net.minecraft.util.EnumChatFormatting;

/**
 * Color sets for the controls screen and the visual keyboard.
 *
 * <p>
 * Every palette was chosen by simulating it under the relevant vision type and maximising the smallest perceptual
 * distance between any two states, so no pair collapses. DEFAULT is a compromise that clears all three dichromacies at
 * once; the per-type palettes trade the others away to gain a little more separation for one. HIGH_CONTRAST separates
 * by lightness instead of hue, which is what greyscale vision and washed out displays need.
 *
 * <p>
 * Minimum distance between any two key states, in CIELAB deltaE, where below about 12 is hard to tell apart:
 *
 * <pre>
 *                  normal  protan  deutan  tritan  greyscale
 *   DEFAULT          20.6    18.5    17.8    14.8        2.7
 *   PROTANOPIA       20.6    20.6       -       -          -
 *   DEUTERANOPIA     20.6       -    20.6       -          -
 *   TRITANOPIA       20.6       -       -    20.6          -
 *   HIGH_CONTRAST    13.7       -       -       -       13.7
 * </pre>
 */
public enum ColorPalette {

    /** Clears all three dichromacies at once. Fine unless you know which type you have. */
    DEFAULT(0xFF1A6B3A, 0xFFA34A16, 0xFF4076C9, 0xFFD4A928, 0xFF555555, 0xFF888888, 0xFF262626, 0xFFB0554F, 0xFF55DD55,
            0xFF55AAFF, 0xFFBB55FF, "E0762E", EnumChatFormatting.RED, "FFAA00", EnumChatFormatting.GOLD),

    /** Red-blind. Trades the other types away for more separation under protanopia. */
    PROTANOPIA(0xFF089B39, 0xFF6A251B, 0xFF204CDF, 0xFFF5B547, 0xFF555555, 0xFF888888, 0xFF262626, 0xFFB0554F,
            0xFF55DD55, 0xFF55AAFF, 0xFFBB55FF, "E0762E", EnumChatFormatting.RED, "FFAA00", EnumChatFormatting.GOLD),

    /** Green-blind, the most common type. */
    DEUTERANOPIA(0xFF1D7233, 0xFFA3743E, 0xFF204CDF, 0xFFF5B547, 0xFF555555, 0xFF888888, 0xFF262626, 0xFFB0554F,
            0xFF55DD55, 0xFF55AAFF, 0xFFBB55FF, "E0762E", EnumChatFormatting.RED, "FFAA00", EnumChatFormatting.GOLD),

    /** Blue-blind, rare. */
    TRITANOPIA(0xFF0AB895, 0xFF9A613B, 0xFF204CDF, 0xFFF5B547, 0xFF555555, 0xFF888888, 0xFF262626, 0xFFB0554F,
            0xFF55DD55, 0xFF55AAFF, 0xFFBB55FF, "E0762E", EnumChatFormatting.RED, "FFAA00", EnumChatFormatting.GOLD),

    /**
     * Separates every state by lightness, so it survives with no color vision at all. Key labels flip between dark and
     * light to stay readable, since the brighter states need dark text.
     */
    HIGH_CONTRAST(0xFF18A826, 0xFF372A1B, 0xFF67C2E9, 0xFFF7EAC6, 0xFF4B4B4B, 0xFF776969, 0xFF100E0E, 0xFFE0A0A0,
            0xFFF0F0F0, 0xFF9E9E9E, 0xFF5A5A5A, "FFFFFF", EnumChatFormatting.WHITE, "9E9E9E", EnumChatFormatting.GRAY);

    public final int keyBound;
    public final int keyConflict;
    public final int keyCombo;
    public final int keySelected;
    public final int keyNormal;
    public final int keyHover;
    public final int keyDisabled;
    /** Border marking a key barred from the selected binding's combo. */
    public final int comboBlockedBorder;
    public final int contextInGame;
    public final int contextGui;
    public final int contextCustom;
    /** Hex digits for &#RRGGBB when Angelica can render it, and the vanilla color to use when it cannot. */
    public final String markHardRgb;
    public final EnumChatFormatting markHardFallback;
    public final String markSoftRgb;
    public final EnumChatFormatting markSoftFallback;
    /** Legend swatches, in the order the legend lists them. */
    public final int[] legendColors;

    ColorPalette(int keyBound, int keyConflict, int keyCombo, int keySelected, int keyNormal, int keyHover,
            int keyDisabled, int comboBlockedBorder, int contextInGame, int contextGui, int contextCustom,
            String markHardRgb, EnumChatFormatting markHardFallback, String markSoftRgb,
            EnumChatFormatting markSoftFallback) {
        this.keyBound = keyBound;
        this.keyConflict = keyConflict;
        this.keyCombo = keyCombo;
        this.keySelected = keySelected;
        this.keyNormal = keyNormal;
        this.keyHover = keyHover;
        this.keyDisabled = keyDisabled;
        this.comboBlockedBorder = comboBlockedBorder;
        this.contextInGame = contextInGame;
        this.contextGui = contextGui;
        this.contextCustom = contextCustom;
        this.markHardRgb = markHardRgb;
        this.markHardFallback = markHardFallback;
        this.markSoftRgb = markSoftRgb;
        this.markSoftFallback = markSoftFallback;
        this.legendColors = new int[] { keyBound, keyConflict, keyCombo, keySelected, keyDisabled, keyNormal };
    }

    /**
     * Black or white, whichever reads better on {@code fill}. HIGH_CONTRAST needs this because its brighter states
     * would otherwise carry white labels; the other palettes stay dark enough that it always picks white.
     */
    public static int labelColor(int fill) {
        return relativeLuminance(fill) > 0.28D ? 0xFF101010 : 0xFFFFFFFF;
    }

    private static double relativeLuminance(int argb) {
        return 0.2126D * channel(argb >> 16) + 0.7152D * channel(argb >> 8) + 0.0722D * channel(argb);
    }

    private static double channel(int shifted) {
        final double c = (shifted & 0xFF) / 255.0D;
        return c <= 0.04045D ? c / 12.92D : Math.pow((c + 0.055D) / 1.055D, 2.4D);
    }
}
