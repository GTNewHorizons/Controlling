package com.blamejared.controlling.mixins.late;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blamejared.controlling.keybinding.GuiKeyDispatch;

/**
 * NotEnoughItems compat. NEI ASM-replaces {@code GuiContainer.handleKeyboardInput} with a body that routes to
 * {@code GuiContainerManager.handleKeyboardInput} without calling super, so
 * {@link com.blamejared.controlling.mixins.early.MixinGuiScreen} never fires for container GUIs when NEI is installed.
 * This opens the GUI key-dispatch window around NEI's handler instead, restoring combo disambiguation in container
 * GUIs.
 */
@Pseudo
@Mixin(targets = "codechicken.nei.guihook.GuiContainerManager", remap = false)
public abstract class MixinNEIGuiContainerManager {

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
