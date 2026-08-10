package com.blamejared.controlling.mixins.early;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

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
import com.blamejared.controlling.keybinding.GuiKeyDispatch;
import com.blamejared.controlling.keybinding.KeyModifier;
import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

@Mixin(GameSettings.class)
public abstract class MixinGameSettings {

    @Unique
    private static final String KEY_OPTION_PREFIX = "key_";

    /** Description -> bind, rebuilt per save; the per-line scan was quadratic in the binding count. */
    @Unique
    private final Map<String, ComboKeyBinding> controlling$saveLookup = new HashMap<>();

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
            // Old configs stored a single modifier name; map it to that modifier's keycode.
            final int legacyKey = KeyModifier.fromSerializedName(parsed.legacyName).getLeftKeyCode();
            final IntArrayList legacy = new IntArrayList();
            if (legacyKey > 0) {
                legacy.add(legacyKey);
            }
            comboKeyBinding.controlling$setComboKeysRaw(legacy);
        } else {
            comboKeyBinding.controlling$setComboKeysRaw(parsed.comboKeys);
        }
    }

    /**
     * A save can run inside a GUI key event, where getKeyCode() is combo-masked; without suspending it a bind whose
     * combo is not held would be persisted as 0.
     */
    @Inject(method = "saveOptions", at = @At("HEAD"))
    private void controlling$beginSaveOptions(CallbackInfo ci) {
        GuiKeyDispatch.suspend();
        this.controlling$saveLookup.clear();
        for (KeyBinding keyBinding : this.keyBindings) {
            if (keyBinding instanceof ComboKeyBinding combo) {
                this.controlling$saveLookup.putIfAbsent(keyBinding.getKeyDescription(), combo);
            }
        }
    }

    @Inject(method = "saveOptions", at = @At("RETURN"))
    private void controlling$endSaveOptions(CallbackInfo ci) {
        this.controlling$saveLookup.clear();
        GuiKeyDispatch.resume();
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
        final int colon = line.indexOf(':');
        if (colon < 0) {
            return line;
        }
        final ComboKeyBinding bind = this.controlling$getComboBindForOptionKey(line.substring(0, colon));
        if (bind == null) {
            return line;
        }
        final IntList comboKeys = bind.controlling$comboKeysRaw();
        if (comboKeys.isEmpty()) {
            return line;
        }
        return line + ":" + ComboKeyCodec.formatComboKeys(comboKeys);
    }

    @Unique
    private ComboKeyBinding controlling$getComboBindForOptionKey(String optionKey) {
        // optionKey is "key_" + description; the map is keyed on the description alone to avoid a concat per binding.
        return this.controlling$saveLookup.get(optionKey.substring(KEY_OPTION_PREFIX.length()));
    }
}
