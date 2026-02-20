package com.blamejared.controlling.mixins;

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

import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.KeyModifier;

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
    private KeyModifier controlling$keyModifier = KeyModifier.NONE;
    @Unique
    private KeyModifier controlling$defaultKeyModifier = KeyModifier.NONE;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void controlling$onInit(String description, int keyCode, String category, CallbackInfo ci) {
        this.controlling$keyModifier = KeyModifier.NONE;
        this.controlling$defaultKeyModifier = KeyModifier.NONE;
    }

    @Inject(method = "setKeyBindState", at = @At("HEAD"), cancellable = true)
    private static void controlling$setKeyBindState(int keyCode, boolean pressed, CallbackInfo ci) {
        for (KeyBinding keyBinding : keybindArray) {
            if (keyBinding.getKeyCode() == keyCode) {
                ((MixinKeyBinding) (Object) keyBinding).pressed = pressed
                        && controlling$isBindingActiveWithModifier(keyBinding);
            }
        }
        ci.cancel();
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private static void controlling$onTick(int keyCode, CallbackInfo ci) {
        if (keyCode != 0) {
            for (KeyBinding keyBinding : keybindArray) {
                if (keyBinding.getKeyCode() == keyCode && controlling$isBindingActiveWithModifier(keyBinding)) {
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

    @Override
    public KeyModifier controlling$getKeyModifier() {
        return this.controlling$keyModifier;
    }

    @Override
    public KeyModifier controlling$getDefaultKeyModifier() {
        return this.controlling$defaultKeyModifier;
    }

    @Override
    public void controlling$setKeyModifier(KeyModifier keyModifier) {
        this.controlling$keyModifier = keyModifier == null ? KeyModifier.NONE : keyModifier;
    }

    @Override
    public void controlling$setDefaultKeyModifier(KeyModifier keyModifier) {
        this.controlling$defaultKeyModifier = keyModifier == null ? KeyModifier.NONE : keyModifier;
    }

    @Override
    public void controlling$setKeyModifierAndCode(KeyModifier keyModifier, int keyCode) {
        this.controlling$setKeyModifier(keyModifier);
        this.keyCode = keyCode;
    }

    @Override
    public String controlling$getDisplayName() {
        final String keyName = GameSettings.getKeyDisplayString(this.keyCode);
        if (this.controlling$keyModifier == KeyModifier.NONE || this.keyCode == Keyboard.KEY_NONE) {
            return keyName;
        }
        return this.controlling$keyModifier.getDisplayName() + " + " + keyName;
    }

    @Override
    public boolean controlling$conflicts(KeyBinding other) {
        if (other == null) {
            return false;
        }
        if (this.keyCode != other.getKeyCode() || this.keyCode == Keyboard.KEY_NONE) {
            return false;
        }

        final KeyModifier otherModifier = other instanceof ComboKeyBinding combo ? combo.controlling$getKeyModifier()
                : KeyModifier.NONE;
        return this.controlling$keyModifier == otherModifier;
    }

    @Override
    public boolean controlling$hasKeyCodeModifierConflict(KeyBinding other) {
        if (!(other instanceof ComboKeyBinding combo)) {
            return false;
        }
        if (!this.controlling$conflicts(other)) {
            return false;
        }
        return this.controlling$keyModifier != combo.controlling$getKeyModifier();
    }

    @Override
    public boolean controlling$isSetToDefaultValue() {
        return this.keyCode == this.keyCodeDefault
                && this.controlling$keyModifier == this.controlling$defaultKeyModifier;
    }

    @Override
    public void controlling$setToDefault() {
        this.keyCode = this.keyCodeDefault;
        this.controlling$keyModifier = this.controlling$defaultKeyModifier;
    }

    @Override
    public boolean controlling$isModifierActive() {
        if (this.controlling$keyModifier == KeyModifier.NONE) {
            return !KeyModifier.isAnyModifierActive();
        }
        return this.controlling$keyModifier.isActive();
    }

    @Unique
    private static boolean controlling$isBindingActiveWithModifier(KeyBinding keyBinding) {
        return !(keyBinding instanceof ComboKeyBinding combo) || combo.controlling$isModifierActive();
    }
}
