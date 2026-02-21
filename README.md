# Controlling

Controlling is a client-side quality-of-life mod for Minecraft 1.7.10 that replaces the vanilla controls screen with a searchable, filterable keybinding UI and built-in combo key support.

## Features

- Search keybindings by category name, keybind name, or key name.
- Highlight search matches directly in the controls list.
- Filter to only conflicting bindings or only unbound bindings.
- Sort keybindings in vanilla order, A-Z, or Z-A.
- Reset individual keybindings or confirm-reset all keybindings.
- Toggle default movement keys between QWERTY and AZERTY presets.
- Use combo keybindings with modifier keys (`Ctrl`, `Shift`, `Alt`).


Incompatible with ModernKeybinding (`mkb`) because combo support is now built in.

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
