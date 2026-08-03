package com.blamejared.controlling.keybinding;

/**
 * Tracks the brief window during which a GUI is dispatching a keyboard event (GuiScreen.handleKeyboardInput ->
 * keyTyped). While the window is open, KeyBinding.getKeyCode() is made modifier/sibling-aware so GUI code that matches
 * bindings via {@code eventKey == keyBinding.getKeyCode()} disambiguates combos.
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
     * Closes the window for code reading {@code getKeyCode()} for a non-input purpose, such as writing options.txt.
     * Depth counted so a nested suspend cannot reopen it early.
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
     * True while the modifier/sibling decision is being computed. The decision reads {@code KeyBinding.getKeyCode()}
     * internally, so the getKeyCode override must return the real keycode (not the disambiguated one) during this
     * window to avoid infinite recursion.
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
