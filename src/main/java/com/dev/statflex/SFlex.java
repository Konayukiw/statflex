package com.dev.statflex;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = SFlex.MODID, name = SFlex.NAME, version = SFlex.VERSION, clientSideOnly = true)
public class SFlex {
    public static final String MODID = "statflex";
    public static final String NAME = "StatFlex";
    public static final String VERSION = "1.01";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.setProperty("https.protocols", "TLSv1.2");

        MinecraftForge.EVENT_BUS.register(new BwListStats());
        MinecraftForge.EVENT_BUS.register(new DuelsListStats());
        MinecraftForge.EVENT_BUS.register(new Denicker());
        MinecraftForge.EVENT_BUS.register(new AutoGG());

        SettingsManager.load();
        Fetcher.syncFromSettings(SettingsManager.getInstance());

        ApiKeyManager.init();

        Fetcher.register();
    }

}
