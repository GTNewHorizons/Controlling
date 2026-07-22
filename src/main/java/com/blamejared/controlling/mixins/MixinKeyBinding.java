package com.blamejared.controlling.mixins;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.api.KeyContext;
import com.blamejared.controlling.api.KeyContexts;
import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.ComboState;
import com.blamejared.controlling.keybinding.GuiKeyDispatch;
import com.blamejared.controlling.keybinding.KeyModifier;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

@Mixin(KeyBinding.class)
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
    private boolean controlling$allowsComboModifier = true;
    @Unique
    private boolean controlling$allowsMouse = true;
    @Unique
    private boolean controlling$allowsKeyboard = true;
    @Unique
    private int controlling$comboHeldTicks = -1; // -1 = not satisfied; 0 = first tick; increments while held

    @Inject(method = "<init>", at = @At("TAIL"))
    private void controlling$onInit(String description, int keyCode, String category, CallbackInfo ci) {
        this.controlling$comboKeys.clear();
        this.controlling$defaultComboKeys.clear();
    }

    @Inject(method = "setKeyBindState", at = @At("HEAD"), cancellable = true)
    private static void controlling$setKeyBindState(int keyCode, boolean pressed, CallbackInfo ci) {
        if (keyCode != 0) {
            for (KeyBinding keyBinding : keybindArray) {
                if (keyBinding.getKeyCode() == keyCode) {
                    ((MixinKeyBinding) (Object) keyBinding).pressed = pressed
                            && controlling$isBindingActiveWithModifier(keyBinding, keyCode);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private static void controlling$onTick(int keyCode, CallbackInfo ci) {
        if (keyCode != 0) {
            for (KeyBinding keyBinding : keybindArray) {
                if (keyBinding.getKeyCode() == keyCode
                        && controlling$isBindingActiveWithModifier(keyBinding, keyCode)) {
                    ((MixinKeyBinding) (Object) keyBinding).pressTime++;
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "getIsKeyPressed", at = @At("HEAD"), cancellable = true)
    private void controlling$getIsKeyPressed(CallbackInfoReturnable<Boolean> cir) {
        if (!this.controlling$isModifierActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void controlling$isPressed(CallbackInfoReturnable<Boolean> cir) {
        if (!this.controlling$isModifierActive()) {
            this.pressTime = 0;
            cir.setReturnValue(false);
        }
    }

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
    public KeyModifier controlling$getKeyModifier() {
        return controlling$deriveModifier(this.controlling$comboKeys);
    }

    @Override
    public KeyModifier controlling$getDefaultKeyModifier() {
        return controlling$deriveModifier(this.controlling$defaultComboKeys);
    }

    @Override
    public void controlling$setKeyModifier(KeyModifier keyModifier) {
        controlling$applyModifier(this.controlling$comboKeys, keyModifier);
    }

    @Override
    public void controlling$setDefaultKeyModifier(KeyModifier keyModifier) {
        controlling$applyModifier(this.controlling$defaultComboKeys, keyModifier);
    }

    // A list of exactly one modifier keycode reads back as that modifier; anything else is NONE.
    @Unique
    private static KeyModifier controlling$deriveModifier(IntArrayList comboKeys) {
        if (comboKeys.size() != 1) {
            return KeyModifier.NONE;
        }
        return KeyModifier.fromKeyCode(comboKeys.getInt(0));
    }

    @Unique
    private static void controlling$applyModifier(IntArrayList comboKeys, KeyModifier keyModifier) {
        comboKeys.clear();
        if (keyModifier != null && keyModifier != KeyModifier.NONE) {
            comboKeys.add(keyModifier.getLeftKeyCode());
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
    public boolean controlling$allowsComboModifier() {
        return this.controlling$allowsComboModifier;
    }

    @Override
    public void controlling$setAllowsComboModifier(boolean allowsComboModifier) {
        this.controlling$allowsComboModifier = allowsComboModifier;
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
    public void controlling$setKeyModifierAndCode(KeyModifier keyModifier, int keyCode) {
        this.controlling$setKeyModifier(keyModifier);
        this.keyCode = keyCode;
    }

    @Override
    public String controlling$getDisplayName() {
        final String mainName = controlling$keyName(this.keyCode);
        if (this.controlling$comboKeys.isEmpty()) {
            return mainName;
        }
        final StringBuilder sb = new StringBuilder();
        // modifiers first for readability, then other combo keys, then the main key
        controlling$appendKeys(sb, true);
        controlling$appendKeys(sb, false);
        if (this.keyCode != Keyboard.KEY_NONE) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(mainName);
        }
        return sb.toString();
    }

    @Unique
    private void controlling$appendKeys(StringBuilder sb, boolean modifiersOnly) {
        for (int i = 0; i < this.controlling$comboKeys.size(); i++) {
            final int key = this.controlling$comboKeys.get(i);
            if (key == this.keyCode) {
                continue; // skip combo key equal to main key
            }
            final boolean isModifier = KeyModifier.isKeyCodeModifier(key);
            if (isModifier != modifiersOnly) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(controlling$keyName(key));
        }
    }

    // display string for a keycode, honoring the mouse encoding (LMB/RMB/MMB)
    @Unique
    private static String controlling$keyName(int keyCode) {
        if (ControllingApi.isMouseKeyCode(keyCode)) {
            final int button = keyCode - ControllingApi.MOUSE_KEYCODE_OFFSET;
            switch (button) {
                case 0:
                    return "LMB";
                case 1:
                    return "RMB";
                case 2:
                    return "MMB";
                default:
                    return "MB" + button;
            }
        }
        return GameSettings.getKeyDisplayString(keyCode);
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
                : new IntArrayList();
        if (!ComboState.sameKeySet(this.keyCode, this.controlling$comboKeys, other.getKeyCode(), otherCombo)) {
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
        return !ComboState.sameKeySet(
                this.keyCode,
                this.controlling$comboKeys,
                combo.controlling$mainKeyCode(),
                combo.controlling$comboKeysRaw());
    }

    @Override
    public boolean controlling$isSetToDefaultValue() {
        return this.keyCode == this.keyCodeDefault
                && this.controlling$comboKeys.equals(this.controlling$defaultComboKeys);
    }

    @Override
    public void controlling$setToDefault() {
        this.keyCode = this.keyCodeDefault;
        this.controlling$comboKeys.clear();
        this.controlling$comboKeys.addAll(this.controlling$defaultComboKeys);
    }

    @Override
    public boolean controlling$isModifierActive() {
        final KeyModifier modifier = this.controlling$getKeyModifier();
        if (modifier == KeyModifier.NONE) {
            if (KeyModifier.isKeyCodeModifier(this.keyCode)) {
                return KeyModifier.fromKeyCode(this.keyCode).isActive();
            }
            return true;
        }
        return modifier.isActive();
    }

    @Unique
    private static boolean controlling$isBindingActiveWithModifier(KeyBinding keyBinding, int inputKeyCode) {
        if (!(keyBinding instanceof ComboKeyBinding combo)) {
            return true;
        }
        if (combo.controlling$getKeyModifier() == KeyModifier.NONE && keyBinding.getKeyCode() == inputKeyCode
                && KeyModifier.isKeyCodeModifier(inputKeyCode)) {
            return true;
        }
        if (combo.controlling$getKeyModifier() == KeyModifier.NONE
                && controlling$hasActiveModifiedSiblingBinding(keyBinding, inputKeyCode)) {
            return false;
        }
        return combo.controlling$isModifierActive();
    }

    @Unique
    private static boolean controlling$hasActiveModifiedSiblingBinding(KeyBinding keyBinding, int inputKeyCode) {
        for (int i = 0; i < keybindArray.size(); i++) {
            KeyBinding otherBinding = keybindArray.get(i);
            if (otherBinding == keyBinding || otherBinding.getKeyCode() != inputKeyCode) {
                continue;
            }
            if (otherBinding instanceof ComboKeyBinding comboKeyBinding
                    && comboKeyBinding.controlling$getKeyModifier() != KeyModifier.NONE
                    && comboKeyBinding.controlling$getKeyModifier().isActive()) {
                return true;
            }
        }
        return false;
    }
}
