package com.konayuki.statflex;

import com.konayuki.statflex.launch.JVMBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = statflex.MODID, name = statflex.NAME, version = statflex.VERSION, clientSideOnly = true)
public class statflex {

    public static final String MODID = "statflex";
    public static final String NAME = "statflex";
    public static final String VERSION = "2.28";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        JVMBootstrap.startFromForge();
    }

    public static void init() {
        JVMBootstrap.startFromInjection();
    }
}
