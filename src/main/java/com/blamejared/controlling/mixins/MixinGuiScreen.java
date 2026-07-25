package com.blamejared.controlling.mixins;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blamejared.controlling.keybinding.GuiKeyDispatch;

/**
 * Opens the GUI key-dispatch window around {@code GuiScreen.handleKeyboardInput}, so that key matching done inside
 * keyTyped (via {@code eventKey == keyBinding.getKeyCode()}) becomes modifier/sibling-aware. This covers vanilla and
 * modded screens. Container GUIs whose handleKeyboardInput is replaced by NotEnoughItems are handled separately by
 * {@link MixinNEIGuiContainerManager}.
 */
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
