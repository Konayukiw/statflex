package com.konayuki.statflex.features.denick;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Ranks;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Toggles;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.features.bedwars.BedwarsList;
import com.konayuki.statflex.features.duels.Duels;
import com.konayuki.statflex.features.skywars.Skywars;

import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.text.Normalizer;

public class Denick {

    private final Set<String> parsed = new HashSet<>();
    public static final Minecraft mc = Minecraft.getMinecraft();

    private static final Gson gson = new Gson();
    private static final Set<String> nicks = loadHashesFromJson();

    private int sendCooldown = 0;


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

            IChatComponent displayNameComponent = npi.getDisplayName();
            String displayNameText = (displayNameComponent != null)
                    ? displayNameComponent.getFormattedText()
                    : name;

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

        GameProfile profile = npi.getGameProfile();

        if (npi == null || npi.getGameProfile() == null)
            return;

        String name = profile.getName();
        IChatComponent displayNameComponent = npi.getDisplayName();
        String displayNameText = (displayNameComponent != null)
                ? displayNameComponent.getFormattedText()
                : name;
        String clean = cleanName(displayNameText).trim();
        String normalized = clean.replaceAll("\\s+", "");

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

        Debug.log(displayNameText + ", UUID=" + profile.getId() + ", UUID version" + profile.getId().version() + " confirmed not to be an NPC");

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

                String displayName = (npi.getDisplayName() != null)
                        ? cleanName(npi.getDisplayName().getFormattedText())
                        : cleanName(npi.getGameProfile().getName());

                if (nicks.contains(hash)) {
                    Chat.send("§8[§cS§8]§7 Found a nicked player:§c " + displayName);
                    new Thread(() -> BedwarsList.warnNickedPlayer(displayName)).start();
                    return;
                }

                String profileName = json.has("profileName") ? json.get("profileName").getAsString() : "";
                if (profileName.isEmpty())
                    return;

                if (!profileName.contains(displayName)) {
                    Chat.send("§8[§cS§8]§c " + profileName + " §7is nicked as §c" + displayName + "§7!");

                    Locraw.getInstance().requestLocraw(new Locraw.LocrawCallback() {
                        @Override
                        public void onLocrawReceived(String gameType, String mode) {
                            processStats(profileName, gameType, mode);
                        }

                        @Override
                        public void onLocrawTimeout() {
                            Debug.log("Locraw timeout for nicked player stats");
                            new Thread(() -> BedwarsList.warnNickedPlayer(
                                    profileName + " (nicked as " + displayName + ")")).start();
                        }
                    });
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
                String detectedMode = Duels.detectModeFromLocraw(processMode);
                Duels.fetchStats(profileName, detectedMode, true);
                BedwarsList.fetchAndWarn(profileName);
                break;
            case "SKYWARS":
                Skywars.fetchStats(profileName, null);
                BedwarsList.fetchAndWarn(profileName);
                break;
            default:
                Chat.send(Messages.UNKNOWN_GAMEMODE);
                BedwarsList.fetchAndWarn(profileName);
        }
    }

    public static String getSkinData(UUID uuid) {
        if (uuid == null) {
            Chat.send("§8[§cS§8]§7 Failed to get UUID");
            return "{}";
        }

        try {
            String urlStr = "https://sessionserver.mojang.com/session/minecraft/profile/"
                    + uuid.toString().replace("-", "") + "?unsigned=false";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int response = conn.getResponseCode();
            if (response != 200) {
                Chat.send("§8[§cS§8]§7 Failed to fetch skin data: " + response);
                return "{}";
            }

            JsonObject root = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);

            if (!root.has("properties") || root.getAsJsonArray("properties").size() == 0) {
                Chat.send("§8[§cS§8]§7 No properties found");
                return "{}";
            }

            JsonObject property = root.getAsJsonArray("properties").get(0).getAsJsonObject();
            if (!property.has("value")) {
                Chat.send("§8[§cS§8]§7 Data in properties was null");
                return "{}";
            }

            String value = property.get("value").getAsString();
            String decoded = new String(Base64.getDecoder().decode(value));
            return decoded;
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private static final Pattern BW_TEAM_LINE = Pattern.compile(
            "\\b([RBGYAWPS])\\b\\s*[:]?\\s*(RED|BLUE|GREEN|YELLOW|AQUA|WHITE|PINK|GRAY)\\b");

    private static String sbU(String s) {
        if (s == null)
            return "";
        s = Ranks.stripColor(s);
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ');
        s = s.replaceAll("[\\p{Cf}\\p{Mn}\\p{Me}]", "");
        s = s.replaceAll("[^\\p{Alnum}:\\s]", "");
        return s.toUpperCase();
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
