package com.blamejared.controlling.keybinding;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

/**
 * Tracks the brief window during which a GUI is dispatching a keyboard event ({@link GuiScreen#handleKeyboardInput} ->
 * {@link GuiScreen#keyTyped(char, int)}). While the window is open, {@link KeyBinding#getKeyCode()} is made
 * modifier/sibling-aware so GUI code that matches bindings via {@code eventKey == keyBinding.getKeyCode()}
 * disambiguates combos.
 *
 * Single-threaded (should only get called from the client keyboard loop); no synchronization required.
 */
public final class GuiKeyDispatch {

    private static boolean inDispatch = false;
    private static int eventKey = 0;
    private static boolean evaluating = false;
    private static int suspendDepth = 0;

    private GuiKeyDispatch() {}

    /**
     * Closes the window for code reading {@link KeyBinding#getKeyCode()} for a non-input purpose, such as
     * {@link net.minecraft.client.settings.GameSettings#saveOptions()} serializing binds to options.txt, where the raw
     * keycode is required rather than the disambiguated one. Depth counted so a nested suspend cannot reopen it early.
     */
    public static void suspend() {
        suspendDepth++;
    }

    public static void resume() {
        if (suspendDepth > 0) {
            suspendDepth--;
        }
    }

    /** Open the window for the given LWJGL event key. */
    public static void begin(int key) {
        inDispatch = true;
        eventKey = key;
    }

    /** Close the window. Safe to call when already closed (defensive). */
    public static void end() {
        inDispatch = false;
        eventKey = 0;
    }

    public static boolean inGuiKeyDispatch() {
        return inDispatch && suspendDepth == 0;
    }

    /** The LWJGL event key currently being dispatched to the GUI. */
    public static int guiEventKey() {
        return eventKey;
    }

    /**
     * True while the modifier/sibling decision is being computed. The decision reads {@link KeyBinding#getKeyCode()}
     * internally, so the {@code getKeyCode} mixin that consults this class must return the real keycode (not the
     * disambiguated one) during this window to avoid infinite recursion.
     */
    public static boolean isEvaluating() {
        return evaluating;
    }

    public static void beginEvaluating() {
        evaluating = true;
    }

    public static void endEvaluating() {
        evaluating = false;
    }
}
