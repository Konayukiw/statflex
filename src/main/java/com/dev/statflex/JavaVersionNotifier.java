/*

package com.dev.statflex;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;

public class JavaVersionNotifier {
    private boolean notified = false;

    @SubscribeEvent
    public void onServerJoin(ClientConnectedToServerEvent event) {
        if (notified)
            return;

        String javaVersion = System.getProperty("java.version");

        if (javaVersion.startsWith("1.8.0_")) {
            try {
                int update = Integer.parseInt(javaVersion.split("_")[1]);
                if (update < 161) {
                    sendMessage("§8[§cS§8]§7 StatFlex requires §4Java 8§7! Your Java version: " + javaVersion);
                    sendMessage(
                            "§c ||§7 Install Java from§f https://adoptium.net/temurin/releases?version=8&os=any&arch=any.");
                    sendMessage("§c ||§7 Make sure 'Add to PATH' and 'Set JAVA_HOME' are enabled at installation.");
                }
            } catch (Exception e) {
                sendMessage("§8[§cS§8] Failed to check Java version (Ignore this).");
            }
        }
        notified = true;
    }

    private void sendMessage(String msg) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
        }
    }
}
*/