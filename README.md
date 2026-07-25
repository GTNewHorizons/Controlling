# Controlling

Controlling is a client-side quality-of-life mod for Minecraft 1.7.10 that replaces the vanilla controls screen with a searchable, filterable keybinding UI and built-in combo key support.

## Features

- Search keybindings by category name, keybind name, or key name.
- Highlight search matches directly in the controls list.
- Filter to only conflicting bindings or only unbound bindings.
- Sort keybindings in vanilla order, A-Z, or Z-A.
- Reset individual keybindings or confirm-reset all keybindings.
- Toggle default movement keys between QWERTY and AZERTY presets.
- Use combo keybindings with modifier keys (`Ctrl`, `Shift`, `Alt`), correctly disambiguated from bare keybindings even while a GUI is open.
- Build N-key chords: a main key plus any number of extra held keys, including non-modifiers and mouse buttons. Hold the chord and press the final key in the controls screen to capture it.
- Resolve combos (including mouse-button combos) while a GUI is open, via a central client-tick poller.
- Bind keys from a visual keyboard overlay with main, numpad, and auxiliary key pages.


Incompatible with ModernKeybinding (`mkb`) because combo support is now built in.

## Credits

The visual keyboard overlay includes code adapted from [Keyboard Wizard](https://github.com/VulpesStella/KeyboardWizard-Legacy), originally by MrNerdy42 and backported by TachibanaSherry. See `THIRD_PARTY_NOTICES.md` for the MIT license notice.

## API

Controlling exposes a small client-side API for combo keybindings in `com.blamejared.controlling.api.ControllingApi`.

```java
import com.blamejared.controlling.api.ComboModifier;
import com.blamejared.controlling.api.ControllingApi;
import org.lwjgl.input.Keyboard;

// Set Ctrl as the default modifier for a key whose default key code is Keyboard.KEY_G.
ControllingApi.setDefaultComboKeyBinding(myKeyBinding, ComboModifier.CONTROL);

// Set the runtime combo binding to Shift + G.
ControllingApi.setComboKeyBinding(myKeyBinding, ComboModifier.SHIFT, Keyboard.KEY_G);
```

### N-key combos and mouse buttons

Beyond the single-modifier convenience layer, a binding carries an ordered list of extra "combo" keys held alongside the main key. Any keycode works, including mouse buttons. A mouse button `b` encodes as keycode `b - 100` (LMB `0` -> `-100`); use the helpers instead of hardcoding the offset.

```java
import java.util.Arrays;
import com.blamejared.controlling.api.ControllingApi;
import org.lwjgl.input.Keyboard;

// Chord: Ctrl + Space + left mouse button as the main key.
ControllingApi.setComboKeys(myKeyBinding, Arrays.asList(Keyboard.KEY_LCONTROL, Keyboard.KEY_SPACE));
// mouseButtonToKeyCode(0) == -100 (LMB); isMouseKeyCode(-100) == true.
```

### Held-state API

A client-tick poller tracks how long each combo has been satisfied (it works inside GUIs and never touches vanilla `pressed`/`pressTime`). Consumers query held-state instead of polling raw input. All return `-1`/`false` for non-combo bindings.

```java
// First-press this tick (edge trigger).
if (ControllingApi.isComboFirstPressed(moveAllBind)) { /* ... */ }

// First press, then repeat once held at least 15 ticks (key-repeat style).
if (ControllingApi.isComboPressedOrHeld(moveSingleBind, 15)) { /* ... */ }

// Raw counter: -1 not held, 0 first tick, then increments.
int ticks = ControllingApi.getComboHeldTicks(myKeyBinding);
```

Note: the poller reads raw key state, so a combo can fire while a GUI text field is focused. Gate that in your handler if it matters.

### Keybinding conflict contexts

Controlling assigns each keybinding a conflict *context* so that bindings on the same key only conflict when they truly clash. Built-in contexts are `UNIVERSAL`, `IN_GAME`, and `GUI` (`com.blamejared.controlling.api.KeyContexts`). Vanilla movement/attack/use binds default to `IN_GAME`; most others stay `UNIVERSAL`.

Mods can set a binding's context, register custom contexts, and block combo modifiers on modifier-tied binds:

```java
import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.api.KeyContexts;

// Only conflicts with other GUI-context binds.
ControllingApi.setKeyConflictContext(myGuiKeyBinding, KeyContexts.GUI);

// Prevent users from attaching Ctrl/Shift/Alt to a modifier-tied bind.
ControllingApi.setAllowsComboModifier(myModifierTiedBinding, false);
```
