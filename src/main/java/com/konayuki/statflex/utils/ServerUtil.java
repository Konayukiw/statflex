package com.konayuki.statflex.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerUtil {
    private ServerUtil() {
    }

    public static boolean isHypixelServer() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.isSingleplayer()) {
            return false;
        }
        ServerData server = minecraft.getCurrentServerData();
        if (server == null || server.serverIP == null) {
            return false;
        }
        String host = server.serverIP.trim().toLowerCase();
        int port = host.indexOf(':');
        if (port >= 0) {
            host = host.substring(0, port);
        }
        return "hypixel.net".equals(host) || host.endsWith(".hypixel.net");
    }
}