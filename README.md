# Controlling

Adds a search bar to the Key-Bindings menu

## API

Controlling exposes a small client-side API for combo keybinds in `com.blamejared.controlling.api.ControllingApi`.

```java
// Set Ctrl as the default modifier for a key whose default key code is Keyboard.KEY_G.
ControllingApi.setDefaultComboKeyBinding(myKeyBinding, ComboModifier.CONTROL);
```
