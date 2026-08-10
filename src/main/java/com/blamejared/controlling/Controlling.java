package com.blamejared.controlling;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.blamejared.controlling.events.ClientEventHandler;
import com.blamejared.controlling.events.ComboPoller;
import com.blamejared.controlling.keybinding.VanillaKeyContexts;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(
        modid = Controlling.MODID,
        name = "Controlling",
        version = Tags.VERSION,
        acceptableRemoteVersions = "*",
        dependencies = "required-after:gtnhlib@[0.9.0,)")
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
        FMLCommonHandler.instance().bus().register(new ComboPoller());

        VanillaKeyContexts.apply();
    }
}
