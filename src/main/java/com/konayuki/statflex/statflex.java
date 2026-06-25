package com.konayuki.statflex;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = statflex.MODID, name = statflex.NAME, version = statflex.VERSION, clientSideOnly = true)
public class statflex {

    public static final String MODID = "statflex";
    public static final String NAME = "statflex";
    public static final String VERSION = "2.12";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        JVMBoostrap.startFromForge();
    }

    public static void init() {
        JVMBoostrap.startFromInjection();
    }
}
