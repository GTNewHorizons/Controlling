package com.blamejared.controlling;

import net.minecraftforge.common.MinecraftForge;

import com.blamejared.controlling.events.ClientEventHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = "controlling", name = "Controlling", version = "GRADLETOKEN_VERSION", acceptableRemoteVersions = "*")
public class Controlling {

    // public static Set<String> PATRON_LIST = new HashSet<>();

    // public Controlling() {
    // new Thread(() -> {
    // try {
    // URL url = new URL("https://blamejared.com/patrons.txt");
    // URLConnection urlConnection = url.openConnection();
    // urlConnection.setConnectTimeout(15000);
    // urlConnection.setReadTimeout(15000);
    // urlConnection.setRequestProperty("User-Agent", "Controlling|1.7.10");
    // try (BufferedReader reader = new BufferedReader(
    // new InputStreamReader(urlConnection.getInputStream()))) {
    // PATRON_LIST = reader.lines().filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    // }
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }).start();
    // }

    @Mod.EventHandler
    private void init(final FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        }
    }
}
