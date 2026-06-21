package com.blamejared.controlling.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.blamejared.controlling.keybinding.GuiKeyDispatch;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * Opens the GUI key-dispatch window around the single vanilla call site that dispatches a keyboard event to the current
 * screen ({@code Minecraft.runTick -> currentScreen.handleKeyboardInput()}). Bracketing here (rather than inside
 * GuiScreen.handleKeyboardInput) covers every screen regardless of which override runs - including NotEnoughItems,
 * which ASM-replaces GuiContainer.handleKeyboardInput without calling super and would otherwise bypass a
 * GuiScreen-level hook.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @WrapOperation(
            method = "runTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;handleKeyboardInput()V"))
    private void controlling$bracketGuiKeyDispatch(GuiScreen screen, Operation<Void> original) {
        GuiKeyDispatch.end();
        GuiKeyDispatch.begin(Keyboard.getEventKey());
        try {
            original.call(screen);
        } finally {
            GuiKeyDispatch.end();
        }
    }
}
