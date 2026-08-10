package com.blamejared.controlling.mixins.early;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.blamejared.controlling.api.KeyContext;
import com.blamejared.controlling.api.KeyContexts;
import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.ComboState;
import com.blamejared.controlling.keybinding.GuiKeyDispatch;
import com.blamejared.controlling.keybinding.InputState;
import com.blamejared.controlling.keybinding.KeyModifier;
import com.blamejared.controlling.keybinding.KeyNames;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

// priority 1500: this class overwrites setKeyBindState/onTick, which Hodgepodge also overwrites at the default 1000.
// The higher priority makes Controlling win that merge deterministically instead of by config load order.
@Mixin(value = KeyBinding.class, priority = 1500)
public abstract class MixinKeyBinding implements ComboKeyBinding {

    @Final
    @Shadow
    private static List<KeyBinding> keybindArray;

    @Shadow
    private int keyCode;
    @Final
    @Shadow
    private int keyCodeDefault;
    @Shadow
    private boolean pressed;
    @Shadow
    private int pressTime;

    @Unique
    private final IntArrayList controlling$comboKeys = new IntArrayList();
    @Unique
    private final IntArrayList controlling$defaultComboKeys = new IntArrayList();
    @Unique
    private KeyContext controlling$keyContext = KeyContexts.UNIVERSAL;
    @Unique
    private boolean controlling$allowsCombos = true;
    @Unique
    private final IntOpenHashSet controlling$blockedComboKeys = new IntOpenHashSet();
    @Unique
    private boolean controlling$allowsMouse = true;
    @Unique
    private boolean controlling$allowsKeyboard = true;
    @Unique
    private int controlling$comboHeldTicks = -1; // -1 = not satisfied; 0 = first tick; increments while held

    /**
     * @author Caedis
     * @reason A bind only counts as pressed when its whole combo is held, its context is active, and no more specific
     *         sibling wins. Vanilla dispatches through a single-binding hash lookup, which cannot express any of that,
     *         so the whole method is replaced rather than injected into.
     *         <p>
     *         Three mods replace this method. NEI rewrites the body from a coremod ASM transformer, Hodgepodge
     *         overwrites it via mixin at the default priority, and this does so at 1500. Coremod transformers run
     *         before the mixin transformer and higher priority merges last, so the final body is always this one. An
     *         {@link Overwrite} is used rather than a cancelling inject so a fourth mod is reported by Mixin as a
     *         conflict instead of being silently discarded.
     */
    @Overwrite
    public static void setKeyBindState(int keyCode, boolean pressed) {
        if (keyCode == 0) {
            return;
        }
        for (int i = 0; i < keybindArray.size(); i++) {
            final KeyBinding keyBinding = keybindArray.get(i);
            final int mainKey = keyBinding.getKeyCode();
            if (mainKey == keyCode) {
                ((MixinKeyBinding) (Object) keyBinding).pressed = pressed
                        && controlling$isBindingActiveWithModifier(keyBinding, keyCode)
                        && controlling$contextActive(keyBinding);
                continue;
            }
            // A combo member changed, so binds on other main keys may have become (un)satisfied. A held main key emits
            // no event, so without this releasing Ctrl leaves a bare W bind suppressed until W is pressed again.
            if (mainKey != ComboState.KEY_NONE && InputState.isDown(mainKey)) {
                ((MixinKeyBinding) (Object) keyBinding).pressed = controlling$isBindingActiveWithModifier(
                        keyBinding,
                        mainKey) && controlling$contextActive(keyBinding);
            }
        }
    }

    /**
     * @author Caedis
     * @reason Combo-aware replacement of the press-time counter; see {@link #setKeyBindState(int, boolean)} for why
     *         this is an overwrite. Only the pressed key's own bindings tick, so completing a combo with a modifier
     *         does not manufacture an extra edge for isPressed().
     */
    @Overwrite
    public static void onTick(int keyCode) {
        if (keyCode == 0) {
            return;
        }
        for (int i = 0; i < keybindArray.size(); i++) {
            final KeyBinding keyBinding = keybindArray.get(i);
            if (keyBinding.getKeyCode() == keyCode && controlling$isBindingActiveWithModifier(keyBinding, keyCode)
                    && controlling$contextActive(keyBinding)) {
                ((MixinKeyBinding) (Object) keyBinding).pressTime++;
            }
        }
    }

    @ModifyReturnValue(method = "getIsKeyPressed", at = @At("RETURN"))
    private boolean controlling$getIsKeyPressed(boolean original) {
        return original && this.controlling$isModifierActive() && this.controlling$keyContext.isActive();
    }

    @ModifyReturnValue(method = "isPressed", at = @At("RETURN"))
    private boolean controlling$isPressed(boolean original) {
        if (this.controlling$isModifierActive() && this.controlling$keyContext.isActive()) {
            return original;
        }
        // Vanilla already consumed a press tick; drop the rest so the bind cannot fire later.
        this.pressTime = 0;
        return false;
    }

    @Unique
    private static boolean controlling$contextActive(KeyBinding keyBinding) {
        return !(keyBinding instanceof ComboKeyBinding combo) || combo.controlling$getKeyContext().isActive();
    }

    /**
     * Not gated on {@link KeyContext#isActive()}: this only disambiguates combos, and masking IN_GAME binds would hide
     * keys like sneak from every mod GUI that looks them up. Firing is gated in setKeyBindState/onTick/isPressed.
     */
    @ModifyReturnValue(method = "getKeyCode", at = @At("RETURN"))
    private int controlling$adjustKeyCodeInGui(int original) {
        if (!GuiKeyDispatch.inGuiKeyDispatch()) {
            return original;
        }
        // The decision below calls getKeyCode() internally; while it runs, those inner calls must see the real keycode
        // and not re-enter this override (which would recurse infinitely).
        if (GuiKeyDispatch.isEvaluating()) {
            return original;
        }
        if (original != GuiKeyDispatch.guiEventKey()) {
            return original;
        }
        GuiKeyDispatch.beginEvaluating();
        try {
            if (!controlling$isBindingActiveWithModifier((KeyBinding) (Object) this, original)) {
                return Keyboard.KEY_NONE;
            }
            return original;
        } finally {
            GuiKeyDispatch.endEvaluating();
        }
    }

    @Override
    public List<Integer> controlling$getComboKeys() {
        final List<Integer> out = new ArrayList<>(this.controlling$comboKeys.size());
        for (int i = 0; i < this.controlling$comboKeys.size(); i++) {
            out.add(this.controlling$comboKeys.getInt(i));
        }
        return out;
    }

    @Override
    public void controlling$setComboKeys(List<Integer> keys) {
        controlling$copyInto(this.controlling$comboKeys, keys);
    }

    @Override
    public void controlling$setDefaultComboKeys(List<Integer> keys) {
        controlling$copyInto(this.controlling$defaultComboKeys, keys);
    }

    @Override
    public void controlling$setComboKeysRaw(IntList keys) {
        this.controlling$comboKeys.clear();
        if (keys != null) {
            this.controlling$comboKeys.addAll(keys);
        }
    }

    @Override
    public IntList controlling$comboKeysRaw() {
        return this.controlling$comboKeys;
    }

    @Override
    public int controlling$mainKeyCode() {
        return this.keyCode;
    }

    @Override
    public int controlling$getComboHeldTicks() {
        return this.controlling$comboHeldTicks;
    }

    @Override
    public void controlling$setComboHeldTicks(int ticks) {
        this.controlling$comboHeldTicks = ticks;
    }

    @Unique
    private static void controlling$copyInto(IntArrayList target, List<Integer> keys) {
        target.clear();
        if (keys == null) {
            return;
        }
        for (Integer key : keys) {
            if (key != null) {
                target.add(key);
            }
        }
    }

    @Override
    public KeyContext controlling$getKeyContext() {
        return this.controlling$keyContext;
    }

    @Override
    public void controlling$setKeyContext(KeyContext keyContext) {
        this.controlling$keyContext = keyContext == null ? KeyContexts.UNIVERSAL : keyContext;
    }

    @Override
    public boolean controlling$allowsCombos() {
        return this.controlling$allowsCombos;
    }

    @Override
    public IntSet controlling$blockedComboKeys() {
        return this.controlling$blockedComboKeys;
    }

    @Override
    public void controlling$setBlockedComboKeys(IntList keys) {
        this.controlling$blockedComboKeys.clear();
        if (keys != null) {
            this.controlling$blockedComboKeys.addAll(keys);
        }
    }

    @Override
    public void controlling$setBlockedComboKeys(int[] keys) {
        this.controlling$blockedComboKeys.clear();
        if (keys != null) {
            this.controlling$blockedComboKeys.addAll(IntArrayList.wrap(keys));
        }
    }

    @Override
    public void controlling$setAllowsCombos(boolean allowsCombos) {
        this.controlling$allowsCombos = allowsCombos;
    }

    @Override
    public boolean controlling$allowsMouse() {
        return this.controlling$allowsMouse;
    }

    @Override
    public void controlling$setAllowsMouse(boolean allowsMouse) {
        this.controlling$allowsMouse = allowsMouse;
    }

    @Override
    public boolean controlling$allowsKeyboard() {
        return this.controlling$allowsKeyboard;
    }

    @Override
    public void controlling$setAllowsKeyboard(boolean allowsKeyboard) {
        this.controlling$allowsKeyboard = allowsKeyboard;
    }

    @Override
    public String controlling$getDisplayName() {
        final String mainName = KeyNames.display(this.keyCode);
        if (this.controlling$comboKeys.isEmpty()) {
            return mainName;
        }
        final StringBuilder sb = new StringBuilder();
        // modifiers first for readability, then other combo keys, then the main key
        controlling$appendKeys(sb, true);
        controlling$appendKeys(sb, false);
        if (this.keyCode != Keyboard.KEY_NONE) {
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append(mainName);
        }
        return sb.toString();
    }

    @Unique
    private void controlling$appendKeys(StringBuilder sb, boolean modifiersOnly) {
        for (int i = 0; i < this.controlling$comboKeys.size(); i++) {
            final int key = this.controlling$comboKeys.getInt(i);
            if (key == this.keyCode) {
                continue; // skip combo key equal to main key
            }
            final boolean isModifier = KeyModifier.isKeyCodeModifier(key);
            if (isModifier != modifiersOnly) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append(KeyNames.display(key));
        }
    }

    @Override
    public boolean controlling$conflicts(KeyBinding other) {
        if (other == null) {
            return false;
        }
        if (this.keyCode != other.getKeyCode() || this.keyCode == Keyboard.KEY_NONE) {
            return false;
        }

        final IntList otherCombo = other instanceof ComboKeyBinding combo ? combo.controlling$comboKeysRaw()
                : IntLists.EMPTY_LIST;
        // Q vs Ctrl+Q is not a clash: most-specific-wins suppresses the shorter one, so only one ever fires.
        if (ComboState.resolvedByPrecedence(this.keyCode, this.controlling$comboKeys, other.getKeyCode(), otherCombo)) {
            return false;
        }

        final KeyContext otherContext = other instanceof ComboKeyBinding comboCtx ? comboCtx.controlling$getKeyContext()
                : KeyContexts.UNIVERSAL;
        return controlling$contextsConflict(this.controlling$keyContext, otherContext);
    }

    @Unique
    private static boolean controlling$contextsConflict(KeyContext a, KeyContext b) {
        return a.conflicts(b) || b.conflicts(a);
    }

    @Override
    public boolean controlling$hasKeyCodeModifierConflict(KeyBinding other) {
        if (!(other instanceof ComboKeyBinding combo)) {
            return false;
        }
        if (!this.controlling$conflicts(other)) {
            return false;
        }
        // Softer case: the combos are incomparable (Ctrl+Q vs Shift+Q), so both fire only while the union is held.
        return !ComboState.sameKeySet(
                this.keyCode,
                this.controlling$comboKeys,
                combo.controlling$mainKeyCode(),
                combo.controlling$comboKeysRaw());
    }

    @Override
    public boolean controlling$isSetToDefaultValue() {
        // Capture order is not meaningful, so compare as sets: Ctrl+Shift equals Shift+Ctrl.
        return this.keyCode == this.keyCodeDefault && ComboState
                .sameKeySet(this.keyCode, this.controlling$comboKeys, this.keyCode, this.controlling$defaultComboKeys);
    }

    @Override
    public void controlling$setToDefault() {
        this.keyCode = this.keyCodeDefault;
        this.controlling$comboKeys.clear();
        this.controlling$comboKeys.addAll(this.controlling$defaultComboKeys);
    }

    @Override
    public boolean controlling$isModifierActive() {
        // No combo keys: keep vanilla behavior, but if the main key is itself a modifier gate on it being held.
        if (this.controlling$comboKeys.isEmpty()) {
            if (KeyModifier.isKeyCodeModifier(this.keyCode)) {
                return KeyModifier.fromKeyCode(this.keyCode).isActive();
            }
            return true;
        }
        // Every combo key must be held (any keycode, including non-modifiers and mouse buttons).
        return controlling$allComboKeysDown(this.controlling$comboKeys);
    }

    @Unique
    private static boolean controlling$isBindingActiveWithModifier(KeyBinding keyBinding, int inputKeyCode) {
        if (!(keyBinding instanceof ComboKeyBinding combo)) {
            return true;
        }
        // A bare modifier main key with no combo keys fires on that key press, without waiting on polled key state.
        final boolean bareModifierPress = combo.controlling$comboKeysRaw().isEmpty()
                && keyBinding.getKeyCode() == inputKeyCode
                && KeyModifier.isKeyCodeModifier(inputKeyCode);
        // All combo keys must be held.
        if (!bareModifierPress && !combo.controlling$isModifierActive()) {
            return false;
        }
        // Most-specific-wins: a satisfied strict-superset sibling on the same key suppresses this bind.
        if (controlling$hasActiveSupersetSibling(keyBinding, inputKeyCode)) {
            return false;
        }
        return true;
    }

    /**
     * Runs off a key event, so only siblings sharing {@code inputKeyCode} as their main key count. Unlike
     * {@link #controlling$hasSatisfiedSuperset(KeyBinding)}, a superset on a different main key does not suppress here.
     */
    @Unique
    private static boolean controlling$hasActiveSupersetSibling(KeyBinding keyBinding, int inputKeyCode) {
        if (!(keyBinding instanceof ComboKeyBinding self)) {
            return false;
        }
        for (int i = 0; i < keybindArray.size(); i++) {
            KeyBinding otherBinding = keybindArray.get(i);
            if (otherBinding == keyBinding || otherBinding.getKeyCode() != inputKeyCode) {
                continue;
            }
            if (otherBinding instanceof ComboKeyBinding otherCombo
                    && ComboState.isStrictSuperset(
                            otherBinding.getKeyCode(),
                            otherCombo.controlling$comboKeysRaw(),
                            self.controlling$mainKeyCode(),
                            self.controlling$comboKeysRaw())
                    && controlling$contextActive(otherBinding)
                    && controlling$allComboKeysDown(otherCombo.controlling$comboKeysRaw())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean controlling$isComboDown() {
        return ComboState.satisfied(this.keyCode, this.controlling$comboKeys, InputState.IS_DOWN);
    }

    @Override
    public boolean controlling$isComboActive() {
        return this.controlling$keyContext.isActive() && this.controlling$isComboDown()
                && !controlling$hasSatisfiedSuperset((KeyBinding) (Object) this);
    }

    /**
     * Most-specific-wins for a direct state query: any other binding whose key set strictly contains this one's, is
     * fully held, and could fire right now, suppresses this one. Unlike
     * {@link #controlling$hasActiveSupersetSibling(KeyBinding, int)} there is no event key to filter on, so every
     * binding is considered.
     */
    @Unique
    private static boolean controlling$hasSatisfiedSuperset(KeyBinding keyBinding) {
        if (!(keyBinding instanceof ComboKeyBinding self)) {
            return false;
        }
        for (int i = 0; i < keybindArray.size(); i++) {
            final KeyBinding other = keybindArray.get(i);
            if (other == keyBinding || !(other instanceof ComboKeyBinding otherCombo)) {
                continue;
            }
            if (ComboState.isStrictSuperset(
                    otherCombo.controlling$mainKeyCode(),
                    otherCombo.controlling$comboKeysRaw(),
                    self.controlling$mainKeyCode(),
                    self.controlling$comboKeysRaw()) && otherCombo.controlling$getKeyContext().isActive()
                    && otherCombo.controlling$isComboDown()) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean controlling$allComboKeysDown(IntList comboKeys) {
        for (int i = 0; i < comboKeys.size(); i++) {
            if (!InputState.isDown(comboKeys.getInt(i))) {
                return false;
            }
        }
        return true;
    }
}
