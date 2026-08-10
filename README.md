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
- Bind keys from a visual keyboard overlay with main, numpad, and auxiliary key pages.


Incompatible with ModernKeybinding (`mkb`) because combo support is now built in.

## Credits

The visual keyboard overlay includes code adapted from [Keyboard Wizard](https://github.com/VulpesStella/KeyboardWizard-Legacy), originally by MrNerdy42 and backported by TachibanaSherry. See `THIRD_PARTY_NOTICES.md` for the MIT license notice.

## API

Controlling exposes a small client-side API for combo keybindings in `com.blamejared.controlling.api.ControllingApi`.
Every `KeyBinding` gains combo support, so these calls work on any binding, yours or another mod's.

```java
import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.api.KeyContexts;
import java.util.Arrays;
import org.lwjgl.input.Keyboard;

// Default the binding to Ctrl + G. Bindings still on their old default are moved to the new one.
ControllingApi.setDefaultComboKeys(myKeyBinding, Arrays.asList(Keyboard.KEY_LCONTROL));

// Set the live binding to Shift + G in one call, so it is never briefly half applied.
ControllingApi.setComboKeyBinding(myKeyBinding, Keyboard.KEY_G, Arrays.asList(Keyboard.KEY_LSHIFT));

// Only conflict with, and only fire alongside, other in-game binds.
ControllingApi.setKeyConflictContext(myKeyBinding, KeyContexts.IN_GAME);

// "LShift+G", the same string the controls screen shows.
String label = ControllingApi.getDisplayName(myKeyBinding);

// Live poll, valid in game and inside any GUI. isComboDown ignores context and more
// specific binds; isComboActive is the "would it fire right now" question.
if (ControllingApi.isComboActive(myKeyBinding)) {
    // ...
}
```

Custom conflict contexts are registered through `com.blamejared.controlling.api.KeyContextRegistry`.
