package com.blamejared.controlling;

import net.minecraftforge.common.MinecraftForge;

import com.blamejared.controlling.events.ClientEventHandler;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = "controlling", name = "Controlling", version = "GRADLETOKEN_VERSION", acceptableRemoteVersions = "*")
public class Controlling {

    public static boolean isModernKeybindingInstalled = false;

    @Mod.EventHandler
    private void init(final FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            isModernKeybindingInstalled = Loader.isModLoaded("mkb");

            MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        }
    }
}
