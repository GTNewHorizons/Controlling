package com.blamejared.controlling;

import net.minecraftforge.common.MinecraftForge;

import com.blamejared.controlling.events.ClientEventHandler;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = "controlling", name = "Controlling", version = Tags.VERSION, acceptableRemoteVersions = "*")
public class Controlling {

    @Mod.EventHandler
    private void init(final FMLInitializationEvent event) {
        if (event.getSide().isServer()) return;

        if (Loader.isModLoaded("mkb")) {
            throw new IllegalStateException(
                    "Controlling now ships built-in key combo support and is incompatible with ModernKeybinding (mkb).");
        }

        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }
}
