package com.blamejared.controlling.mixins;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blamejared.controlling.keybinding.GuiKeyDispatch;

/**
 * NotEnoughItems compat. NEI ASM-replaces {@code GuiContainer.handleKeyboardInput} with a body that routes to
 * {@code GuiContainerManager.handleKeyboardInput} without calling super, so {@link MixinGuiScreen} never fires for
 * container GUIs when NEI is installed. This opens the GUI key-dispatch window around NEI's handler instead, restoring
 * combo disambiguation in container GUIs.
 *
 * <p>
 * Soft (string) target: when NEI is absent the target class never loads, so this mixin stays dormant and harmless.
 */
@Mixin(targets = "codechicken.nei.guihook.GuiContainerManager")
public abstract class MixinNEIGuiContainerManager {

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), remap = false)
    private void controlling$openKeyDispatch(CallbackInfo ci) {
        GuiKeyDispatch.end();
        GuiKeyDispatch.begin(Keyboard.getEventKey());
    }

    @Inject(method = "handleKeyboardInput", at = @At("RETURN"), remap = false)
    private void controlling$closeKeyDispatch(CallbackInfo ci) {
        GuiKeyDispatch.end();
    }
}
