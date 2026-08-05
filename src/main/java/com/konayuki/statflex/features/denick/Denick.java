package com.konayuki.statflex.features.denick;

import com.konayuki.statflex.utils.*;
import com.konayuki.statflex.utils.api.Profile;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.chat.Warn;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.utils.hypixel.isBot;
import com.konayuki.statflex.features.bedwars.BedwarsList;
import com.konayuki.statflex.features.duels.Duels;
import com.konayuki.statflex.features.skywars.Skywars;

import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.*;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.Base64;

public class Denick {

    private final Set<String> parsed = new HashSet<>();
    public static final Minecraft mc = Minecraft.getMinecraft();

    private static final Gson gson = new Gson();
    private static final Set<String> nicks = loadHashesFromJson();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (!Toggles.isDenickEnabled())
            return;

        for (NetworkPlayerInfo npi : mc.getNetHandler().getPlayerInfoMap()) {
            String name = npi.getGameProfile().getName();
            if (parsed.contains(name))
                continue;

            parseSkinData(npi);
            parsed.add(name);
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent event) {
        if (event.entity == mc.thePlayer) {
            parsed.clear();
        }
    }

    public void parseSkinData(NetworkPlayerInfo npi) {
        if (npi == null || npi.getGameProfile() == null)
            return;

        GameProfile profile = npi.getGameProfile();
        String name = profile.getName();
        IChatComponent displayNameComponent = npi.getDisplayName();
        String displayNameText = (displayNameComponent != null)
                ? displayNameComponent.getFormattedText()
                : name;
        String clean = cleanName(displayNameText).trim();
        String normalized = clean.replaceAll("\\s+", "");

        if (mc.theWorld != null) {
            EntityPlayer entity = mc.theWorld.getPlayerEntityByUUID(profile.getId());
            if (entity != null && isBot.isBot(entity)) {
                return;
            }
        }

        if (normalized.contains("[NPC]") || name.contains("[NPC]")) {
            return;
        }

        String pingText = String.valueOf(npi.getResponseTime());
        pingText = pingText.replaceAll("§.", "").trim();

        if (pingText.contains("?")) {
            return;
        }

        int ping = npi.getResponseTime();
        if (ping <= 0) {
            return;
        }

        if (profile.getId().version() == 2) {
            return;
        }

        for (Property prop : profile.getProperties().get("textures")) {
            try {
                String value = prop.getValue();
                String decoded = new String(Base64.getDecoder().decode(value));
                JsonObject json = gson.fromJson(decoded, JsonObject.class);

                if (!json.has("textures"))
                    continue;
                JsonObject textures = json.getAsJsonObject("textures");

                if (!textures.has("SKIN"))
                    continue;
                JsonObject skin = textures.getAsJsonObject("SKIN");

                if (!skin.has("url"))
                    continue;
                String hash = skin.get("url").getAsString().split("/")[4];

                String profileName = json.has("profileName") ? json.get("profileName").getAsString() : "";
                if (!profileName.isEmpty() && !profileName.equalsIgnoreCase(name)) {
                    Chat.send("§8[§cS§8]§c " + profileName + " §7is nicked as §c" + name + "§7!");

                    Locraw.getInstance().sendLocraw(new Locraw.LocrawCallback() {
                        @Override
                        public void onLocrawReceived(String gameType, String mode) {
                            processStats(profileName, gameType, mode);
                        }

                        @Override
                        public void onLocrawTimeout() {
                            new Thread(() -> Warn.sendWarning(
                                    profileName + " (nicked as " + name + ")")).start();
                        }
                    });
                    return;
                }

                if (nicks.contains(hash)) {
                    final String checkName = name;
                    new Thread(() -> {
                        Boolean exists = Profile.nameExists(checkName);
                        if (Boolean.TRUE.equals(exists)) {
                            return;
                        }
                        if (Boolean.FALSE.equals(exists)) {
                            Chat.send("§8[§cS§8]§7 Found a nicked player:§c " + checkName);
                        }
                    }, "Denick").start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processStats(String profileName, String gameType, String mode) {
        String processMode = mode != null ? mode.toLowerCase() : null;

        if (gameType == null) {
            Chat.send(Messages.UNKNOWN_GAMEMODE);
            return;
        }

        switch (gameType) {
            case "BEDWARS":
                BedwarsList.listBedwarsStats(Arrays.asList(profileName), true);
                break;
            case "DUELS":
                String detectedMode = Duels.detectMode(processMode);
                Duels.fetchStats(profileName, detectedMode, true);
                break;
            case "SKYWARS":
                Skywars.fetchStats(profileName, null);
                break;
            default:
                Chat.send(Messages.UNKNOWN_GAMEMODE);
        }
    }

    private static String cleanName(String name) {
        return name.replaceAll("§[0-9a-frk-o]", "").trim();
    }

    private static Set<String> loadHashesFromJson() {
        try {
            InputStream inputStream = Denick.class.getResourceAsStream("/hashset.json");
            if (inputStream == null) {
                Debug.error("hashset.json not found in resources");
                return new HashSet<>();
            }
            List<String> hashList = gson.fromJson(new InputStreamReader(inputStream), new TypeToken<List<String>>(){}.getType());
            return new HashSet<>(hashList);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }
}
