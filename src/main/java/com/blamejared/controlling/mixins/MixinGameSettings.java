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
        if (astring.length > 2) {
            if (keybinding instanceof ComboKeyBinding comboKeyBinding) {
                comboKeyBinding.controlling$setKeyModifier(KeyModifier.fromSerializedName(astring[2]));
            }
        }
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

        final KeyModifier keyModifier = this.controlling$getModifierForOptionKey(split[0]);
        if (keyModifier == null || keyModifier == KeyModifier.NONE) {
            return line;
        }
        return split[0] + ":" + split[1] + ":" + keyModifier.name();
    }

    @Unique
    private KeyModifier controlling$getModifierForOptionKey(String optionKey) {
        for (KeyBinding keyBinding : this.keyBindings) {
            if (optionKey.equals(KEY_OPTION_PREFIX + keyBinding.getKeyDescription())
                    && keyBinding instanceof ComboKeyBinding comboKeyBinding) {
                return comboKeyBinding.controlling$getKeyModifier();
            }
        }
        return null;
    }
}
