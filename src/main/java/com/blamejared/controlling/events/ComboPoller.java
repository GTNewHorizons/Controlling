package com.blamejared.controlling.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.ComboKeyBinding;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

/**
 * Maintains a held-tick counter per combo bind from controlling$isComboActive at the end of each client tick. Works
 * while a GUI is open (Keyboard.isKeyDown / Mouse.isButtonDown stay live), so mouse-in-GUI combos resolve here rather
 * than through vanilla dispatch. Only writes controlling$comboHeldTicks; never touches vanilla pressed/pressTime.
 */
public final class ComboPoller {

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || mc.gameSettings.keyBindings == null) {
            return;
        }
        final KeyBinding[] binds = mc.gameSettings.keyBindings;
        for (int i = 0; i < binds.length; i++) {
            final KeyBinding kb = binds[i];
            if (!(kb instanceof ComboKeyBinding combo)) {
                continue;
            }
            final int held = combo.controlling$getComboHeldTicks();
            combo.controlling$setComboHeldTicks(combo.controlling$isComboActive() ? (held < 0 ? 0 : held + 1) : -1);
        }
    }
}
