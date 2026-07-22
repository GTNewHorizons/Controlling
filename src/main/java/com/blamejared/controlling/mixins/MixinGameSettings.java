package com.blamejared.controlling.mixins;

import java.io.File;
import java.io.PrintWriter;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blamejared.controlling.keybinding.ComboKeyBinding;
import com.blamejared.controlling.keybinding.ComboKeyCodec;
import com.blamejared.controlling.keybinding.KeyModifier;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(GameSettings.class)
public abstract class MixinGameSettings {

    @Unique
    private static final String KEY_OPTION_PREFIX = "key_";

    @Shadow
    public KeyBinding[] keyBindings;

    @Shadow
    private File optionsFile;

    @Inject(
            method = "loadOptions",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyCode(I)V"))
    private void controlling$loadOptions(CallbackInfo ci, @Local KeyBinding keybinding, @Local String[] astring) {
        if (astring.length <= 2 || !(keybinding instanceof ComboKeyBinding comboKeyBinding)) {
            return;
        }
        final ComboKeyCodec.Parsed parsed = ComboKeyCodec.parse(astring[2]);
        if (parsed.legacy) {
            comboKeyBinding.controlling$setKeyModifier(KeyModifier.fromSerializedName(parsed.legacyName));
        } else {
            comboKeyBinding.controlling$setComboKeys(controlling$toBoxedList(parsed.comboKeys));
        }
    }

    @Unique
    private static java.util.List<Integer> controlling$toBoxedList(it.unimi.dsi.fastutil.ints.IntList keys) {
        final java.util.List<Integer> out = new java.util.ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            out.add(keys.get(i));
        }
        return out;
    }

    @Redirect(
            method = "saveOptions",
            at = @At(value = "INVOKE", target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V"))
    private void controlling$saveModifierInline(PrintWriter printWriter, String line) {
        if (line.startsWith(KEY_OPTION_PREFIX)) {
            printWriter.println(this.controlling$appendModifierToKeyLine(line));
        } else {
            printWriter.println(line);
        }
    }

    @Unique
    private String controlling$appendModifierToKeyLine(String line) {
        final String[] split = line.split(":", 2);

        final ComboKeyBinding bind = this.controlling$getComboBindForOptionKey(split[0]);
        if (bind == null) {
            return line;
        }
        final it.unimi.dsi.fastutil.ints.IntList comboKeys = bind.controlling$comboKeysRaw();
        if (comboKeys.isEmpty()) {
            return line;
        }
        return split[0] + ":" + split[1] + ":" + ComboKeyCodec.formatComboKeys(comboKeys);
    }

    @Unique
    private ComboKeyBinding controlling$getComboBindForOptionKey(String optionKey) {
        for (KeyBinding keyBinding : this.keyBindings) {
            if (optionKey.equals(KEY_OPTION_PREFIX + keyBinding.getKeyDescription())
                    && keyBinding instanceof ComboKeyBinding comboKeyBinding) {
                return comboKeyBinding;
            }
        }
        return null;
    }
}
