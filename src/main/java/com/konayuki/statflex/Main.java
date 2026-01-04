package com.konayuki.statflex;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION, clientSideOnly = true)

public class Main {
    public static final String MODID = "statflex";
    public static final String NAME = "statflex";
    public static final String VERSION = "1.23";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.setProperty("https.protocols", "TLSv1.2");

        MinecraftForge.EVENT_BUS.register(new BwListStats());
        MinecraftForge.EVENT_BUS.register(new DuelsListStats());
        MinecraftForge.EVENT_BUS.register(new Denicker());
        MinecraftForge.EVENT_BUS.register(new AutoGG());
        MinecraftForge.EVENT_BUS.register(new UpdaterGuiHandler());

        SettingsManager.load();
        Fetcher.syncFromSettings(SettingsManager.getInstance());

        Updater.checkForUpdatesAsync();

        ApiKeyManager.init();

        Fetcher.register();
    }

}
