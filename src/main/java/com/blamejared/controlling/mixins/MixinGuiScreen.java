package com.blamejared.controlling.mixins;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blamejared.controlling.keybinding.GuiKeyDispatch;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"))
    private void controlling$openKeyDispatch(CallbackInfo ci) {
        GuiKeyDispatch.end();
        GuiKeyDispatch.begin(Keyboard.getEventKey());
    }

    @Inject(method = "handleKeyboardInput", at = @At("RETURN"))
    private void controlling$closeKeyDispatch(CallbackInfo ci) {
        GuiKeyDispatch.end();
    }
}
