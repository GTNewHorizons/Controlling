# Controlling

Controlling is a client-side quality-of-life mod for Minecraft 1.7.10 that replaces the vanilla controls screen with a searchable, filterable keybinding UI and built-in combo key support.

## Features

- Search keybindings by category name, keybind name, or key name.
- Highlight search matches directly in the controls list.
- Filter to only conflicting bindings or only unbound bindings.
- Sort keybindings in vanilla order, A-Z, or Z-A.
- Reset individual keybindings or confirm-reset all keybindings.
- Toggle default movement keys between QWERTY and AZERTY presets.
- Use chord keybindings, correctly disambiguated from bare keybindings even while a GUI is open.
- Build N-key chords: a main key plus any number of extra held keys, including non-modifiers and mouse buttons. Hold the chord and press the final key in the controls screen to capture it.
- Resolve combos (including mouse-button combos) while a GUI is open, via a central client-tick poller.
- Bind keys from a visual keyboard overlay with main, numpad, auxiliary key pages and a mouse-button row. Left-click a key to bind it; right-click keys to build the chord that will be attached, shown live in the header with a `Clear` button. Keys already in the chord are highlighted and cannot double as the main key. Holding any keys filters the keyboard to bindings using them; left and right click drive the overlay itself, so put those in a chord from the mouse row instead of holding them.


## Color palettes

The controls list and visual keyboard color-code binding state. `config/controlling.cfg` selects the palette:

| Palette | For |
|---|---|
| `DEFAULT` | readable with any one of the three common types of color blindness |
| `PROTANOPIA` / `DEUTERANOPIA` / `TRITANOPIA` | trades the other types away for more separation in one |
| `HIGH_CONTRAST` | separates by lightness rather than hue, for greyscale vision or a washed out display |

Each was picked by simulating it under the relevant vision type and maximising the smallest perceptual distance between any two states, so no two collapse into each other.

Incompatible with ModernKeybinding (`mkb`) because combo support is now built in.

## Credits

The visual keyboard overlay includes code adapted from [Keyboard Wizard](https://github.com/VulpesStella/KeyboardWizard-Legacy), originally by MrNerdy42 and backported by TachibanaSherry. See `THIRD_PARTY_NOTICES.md` for the MIT license notice.

## API

Controlling exposes a small client-side API for combo keybindings in `com.blamejared.controlling.api.ControllingApi`.

The published `api` artifact holds only `com.blamejared.controlling.api`, and no signature in it names an internal or fastutil type, so it compiles against Minecraft alone. The full mod is still required at runtime.

### Chords

A binding carries a main key plus an ordered list of extra "chord" keys held alongside it. Any keycode works, including mouse buttons and non-modifiers. A mouse button `b` encodes as keycode `b - 100` (LMB `0` -> `-100`); use the helpers instead of hardcoding the offset.

```java
import java.util.Arrays;
import com.blamejared.controlling.api.ControllingApi;
import org.lwjgl.input.Keyboard;

// Ctrl + Space + G, where G is the binding's main key.
ControllingApi.setComboKeys(myKeyBinding, Arrays.asList(Keyboard.KEY_LCONTROL, Keyboard.KEY_SPACE));

// Read it back; the list is empty for a binding with no chord.
List<Integer> chord = ControllingApi.getComboKeys(myKeyBinding);

// mouseButtonToKeyCode(0) == -100 (LMB); isMouseKeyCode(-100) == true.
```

### Setting a whole binding

`setComboKeys` changes only the chord. When the main key changes too, set both at once so the binding is never briefly half applied:

```java
// Ctrl + G, main key and chord together.
ControllingApi.setComboKeyBinding(myKeyBinding, Keyboard.KEY_G, Arrays.asList(Keyboard.KEY_LCONTROL));
```

`setDefaultComboKeys` sets the chord a binding resets to. A binding still sitting on its old default is moved to the new one, so changing a shipped default reaches players who never customised it, while customised bindings are left alone.

### Display strings

For tooltips and help text, ask for the binding's full display string rather than building one from the keycode, so it stays in step with the controls screen.

```java
// "LCtrl+G" for a chorded binding, "G" for a plain one.
String keyText = ControllingApi.getDisplayName(myKeyBinding);
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

### Instant state queries

The held-state counters above update once per client tick. When you need the answer *now* - inside a GUI event handler, a render pass, or anywhere a tick boundary has not passed - poll the input state directly instead. Both work everywhere on the client and return `false` for non-combo bindings.

```java
// Is the whole chord physically held this instant?
if (ControllingApi.isChordDown(myKeyBinding)) { /* ... */ }

// Same, but also requires the binding's context to be active and no more
// specific binding to be held: with G and Ctrl+G bound, holding Ctrl+G
// reports true only for Ctrl+G.
if (ControllingApi.isChordActive(myKeyBinding)) { /* ... */ }
```

`isChordActive` is the same decision the tick poller uses, so `isComboPressed` is its tick-quantized form. Use `isChordDown` when you want the literal key state and will do your own disambiguation.

### Keybinding conflict contexts

Controlling assigns each keybinding a conflict *context* so that bindings on the same key only conflict when they truly clash. Built-in contexts are `UNIVERSAL`, `IN_GAME`, and `GUI` (`com.blamejared.controlling.api.KeyContexts`). Vanilla movement/attack/use binds default to `IN_GAME`; most others stay `UNIVERSAL`.

A context also gates whether a bind may fire: `IN_GAME` only with no screen open, `GUI` only with one open. This applies to vanilla `isPressed()`/`getIsKeyPressed()` as well as to `isChordActive`, so a `GUI` bind does not fire in the world. The one exception is `getKeyCode()`, which stays context-blind so mod GUIs can still look up keys like sneak.

Mods can set a binding's context, register custom contexts, and restrict what the user may put in a chord:

```java
import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.api.KeyContexts;

// Only conflicts with other GUI-context binds.
ControllingApi.setKeyConflictContext(myGuiKeyBinding, KeyContexts.GUI);

// Prevent users from attaching any chord keys to a modifier-tied bind.
ControllingApi.setAllowsChords(myModifierTiedBinding, false);

// Or allow chords generally, but keep one key out of them: useful when the mod
// reads that key itself. Blocking restricts the GUI only, so the mod can still
// ship a default chord via setComboKeys.
ControllingApi.setBlockedChordKeys(myBinding, Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL);
```

To define a custom context, extend `KeyContext` and implement `conflicts`; `isActive` is optional and defaults to always-active. The display name comes from `options.context.<id>` in your lang file, or pass an existing key via the two-argument constructor `KeyContext(String id, String translationKey)`.

```java
// Custom context: conflicts only with itself (plus UNIVERSAL, via the a||b rule).
public static final KeyContext AE2 = new KeyContext("ae2") {

    @Override
    public boolean conflicts(KeyContext other) {
        return other == this;
    }
};

// During client init. Throws IllegalArgumentException on a reserved or duplicate id.
ControllingApi.registerKeyContext(AE2);
```
