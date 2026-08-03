package com.blamejared.controlling.keybinding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.api.KeyContexts;

/** Assigns IN_GAME context to world-only vanilla keybinds; everything else stays UNIVERSAL. */
public final class VanillaKeyContexts {

    private static final Set<String> IN_GAME_DESCRIPTIONS = new HashSet<>(
            Arrays.asList(
                    "key.forward",
                    "key.back",
                    "key.left",
                    "key.right",
                    "key.jump",
                    "key.sneak",
                    "key.sprint",
                    "key.attack",
                    "key.use",
                    "key.pickItem",
                    "key.togglePerspective",
                    "key.smoothCamera",
                    "key.chat",
                    "key.command",
                    "key.playerlist"));

    private VanillaKeyContexts() {}

    /** Applies the curated context map to the current keybindings. Safe to call once at init. */
    public static void apply() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || mc.gameSettings.keyBindings == null) {
            return;
        }
        for (KeyBinding keyBinding : mc.gameSettings.keyBindings) {
            if (keyBinding instanceof ComboKeyBinding comboKeyBinding
                    && IN_GAME_DESCRIPTIONS.contains(keyBinding.getKeyDescription())) {
                comboKeyBinding.controlling$setKeyContext(KeyContexts.IN_GAME);
            }
        }
    }
}
