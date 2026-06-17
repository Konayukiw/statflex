package com.konayuki.statflex;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION, clientSideOnly = true)
public class Main {

    public static final String MODID = "statflex";
    public static final String NAME = "statflex";
    public static final String VERSION = "2.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        StatFlexBootstrap.startFromForge();
    }

    public static void init() {
        StatFlexBootstrap.startFromInjection();
    }
}
