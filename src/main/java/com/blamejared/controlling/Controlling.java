package com.blamejared.controlling;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.blamejared.controlling.api.ControllingApi;
import com.blamejared.controlling.events.ClientEventHandler;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = Controlling.MODID, name = "Controlling", version = Tags.VERSION, acceptableRemoteVersions = "*")
public class Controlling {

    public static final String MODID = "controlling";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    private void init(final FMLInitializationEvent event) {
        if (event.getSide().isServer()) return;

        if (Loader.isModLoaded("mkb")) {
            throw new IllegalStateException(
                    "Controlling now ships built-in key combo support and is incompatible with ModernKeybinding (mkb).");
        }

        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        if (Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            registerDebugKeyBindings();
        }
    }

    // dev env only: binding with modifiers disabled for testing the API
    private static void registerDebugKeyBindings() {
        final KeyBinding noModifiers = new KeyBinding("Debug - No Modifiers", Keyboard.KEY_NONE, "Controlling Debug");
        ClientRegistry.registerKeyBinding(noModifiers);
        ControllingApi.setModifiersDisabled(noModifiers, true);
    }
}
