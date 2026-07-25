package com.blamejared.controlling.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.ComboState;
import com.blamejared.controlling.keybinding.InputState;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

/**
 * Maintains a held-tick counter per combo bind by directly polling key/mouse state at the end of each client tick.
 * Works while a GUI is open (Keyboard.isKeyDown / Mouse.isButtonDown stay live), so mouse-in-GUI combos resolve here
 * rather than through vanilla dispatch. Only writes controlling$comboHeldTicks; never touches vanilla
 * pressed/pressTime.
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
            boolean effective = controlling$rawSatisfied(combo) && !controlling$supersetSatisfied(binds, combo);
            final int held = combo.controlling$getComboHeldTicks();
            combo.controlling$setComboHeldTicks(effective ? (held < 0 ? 0 : held + 1) : -1);
        }
    }

    private static boolean controlling$rawSatisfied(ComboKeyBinding combo) {
        return combo.controlling$getKeyContext().isActive() && ComboState
                .satisfied(combo.controlling$mainKeyCode(), combo.controlling$comboKeysRaw(), InputState.IS_DOWN);
    }

    private static boolean controlling$supersetSatisfied(KeyBinding[] binds, ComboKeyBinding self) {
        for (int i = 0; i < binds.length; i++) {
            final KeyBinding other = binds[i];
            if (other == self || !(other instanceof ComboKeyBinding combo)) {
                continue;
            }
            if (ComboState.isStrictSuperset(
                    combo.controlling$mainKeyCode(),
                    combo.controlling$comboKeysRaw(),
                    self.controlling$mainKeyCode(),
                    self.controlling$comboKeysRaw()) && controlling$rawSatisfied(combo)) {
                return true;
            }
        }
        return false;
    }
}
