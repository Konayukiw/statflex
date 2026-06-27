package com.konayuki.statflex.feature.denick;

import com.konayuki.statflex.client.ChatManager;
import com.konayuki.statflex.config.Toggles;
import com.konayuki.statflex.stats.bedwars.BedwarsStatsLister;
import com.konayuki.statflex.system.Messages;
import com.konayuki.statflex.stats.duels.DuelsFetcher;
import com.konayuki.statflex.stats.skywars.SkywarsFetcher;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
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
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.text.Normalizer;

public class Denicker {

    private final Set<String> parsed = new HashSet<>();
    public static final Minecraft mc = Minecraft.getMinecraft();

    private static final Gson gson = new Gson();
    private static final Set<String> nicks = loadHashesFromJson();

    private int sendCooldown = 0;

    private boolean awaitingLocraw = false;
    private String currentGameType = null;
    private String currentMode = null;
    private String pendingProfile = null;
    private int locrawTimeout = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) {
            return;
        }
        if (!Toggles.denickEnabled)
            return;

        if (awaitingLocraw) {
            locrawTimeout++;
            if (locrawTimeout > 100) {
                awaitingLocraw = false;
                locrawTimeout = 0;
                pendingProfile = null;
                System.err.println("[S] Error: /locraw timeout.");
            }
        }

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
            awaitingLocraw = false;
            currentGameType = null;
            currentMode = null;
            pendingProfile = null;
            locrawTimeout = 0;
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (awaitingLocraw) {
            String message = event.message.getUnformattedText();
            if (message.startsWith("{") && message.endsWith("}")) {
                try {
                    JsonObject json = new JsonParser().parse(message).getAsJsonObject();
                    currentGameType = json.has("gametype") ? json.get("gametype").getAsString() : null;
                    currentMode = json.has("mode") ? json.get("mode").getAsString() : null;
                    System.out.println("[S] Locraw: gametype=" + currentGameType + ", mode=" + currentMode);
                    event.setCanceled(true);
                    awaitingLocraw = false;
                    locrawTimeout = 0;

                    if (pendingProfile != null) {
                        processStats(pendingProfile);
                        pendingProfile = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

        System.out.println("[S] " + displayNameText + ", UUID=" + profile.getId() + ", UUID version" + profile.getId().version() + " confirmed not to be an NPC");

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
                    ChatManager.send("§8[§cS§8]§7 Found a nicked player:§c " + displayName);
                    return;
                }

                String profileName = json.has("profileName") ? json.get("profileName").getAsString() : "";
                if (profileName.isEmpty())
                    return;

                if (!profileName.contains(displayName)) {
                    ChatManager.send("§8[§cS§8]§c " + profileName + " §7is nicked as §c" + displayName + "§7!");

                    if (!awaitingLocraw) {
                        pendingProfile = profileName;
                        mc.thePlayer.sendChatMessage("/locraw");
                        awaitingLocraw = true;
                        locrawTimeout = 0;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processStats(String profileName) {
        String gameType = currentGameType;
        String mode = currentMode != null ? currentMode.toLowerCase() : null;

        if (gameType == null) {
            ChatManager.send(Messages.UNKNOWN_GAMEMODE);
            return;
        }

        switch (gameType) {
            case "BEDWARS":
                BedwarsStatsLister.listBedwarsStats(Arrays.asList(profileName));
                break;
            case "DUELS":
                String detectedMode = detectDuelsMode(Collections.singletonList(mode));
                DuelsFetcher.fetchStats(profileName, detectedMode, true);
                break;
            case "SKYWARS":
                SkywarsFetcher.fetchStats(profileName, null);
                break;
            default:
                ChatManager.send(Messages.UNKNOWN_GAMEMODE);
        }
    }

    public static String getSkinData(UUID uuid) {
        if (uuid == null) {
            ChatManager.send("§8[§cS§8]§7 Failed to get UUID");
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
                ChatManager.send("§8[§cS§8]§7 Failed to fetch skin data: " + response);
                return "{}";
            }

            JsonObject root = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);

            if (!root.has("properties") || root.getAsJsonArray("properties").size() == 0) {
                ChatManager.send("§8[§cS§8]§7 No properties found");
                return "{}";
            }

            JsonObject property = root.getAsJsonArray("properties").get(0).getAsJsonObject();
            if (!property.has("value")) {
                ChatManager.send("§8[§cS§8]§7 Data in properties was null");
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
        s = stripColor(s);
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ');
        s = s.replaceAll("[\\p{Cf}\\p{Mn}\\p{Me}]", "");
        s = s.replaceAll("[^\\p{Alnum}:\\s]", "");
        return s.toUpperCase();
    }

    private String detectDuelsMode(List<String> lines) {
        for (String rawLine : lines) {
            String line = stripColor(rawLine).toLowerCase();
            if (line.contains("blitzsg") || line.contains("blitz")) {
                return "blitz_duel";
            } else if (line.contains("bow")) {
                return "bow_duel";
            } else if (line.contains("bow spleef") || line.contains("tnt")) {
                return "bowspleef_duel";
            } else if (line.contains("spleef")) {
                return "spleef_duel";
            } else if (line.contains("boxing")) {
                return "boxing_duel";
            } else if (line.contains("bridge")) {
                return "bridge_duel";
            } else if (line.contains("classic")) {
                return "classic_duel";
            } else if (line.contains("combo")) {
                return "combo_duel";
            } else if (line.contains("megawalls") || line.contains("mw")) {
                return "mw_duel";
            } else if (line.contains("nodebuff")) {
                return "potion_duel";
            } else if (line.contains("op")) {
                return "op_duel";
            } else if (line.contains("parkour")) {
                return "parkour_duel";
            } else if (line.contains("skywars") || line.contains("sw")) {
                return "sw_duel";
            } else if (line.contains("sumo")) {
                return "sumo_duel";
            } else if (line.contains("uhc")) {
                return "uhc_duel";
            } else if (line.contains("bw") || line.contains("bedwars") || line.contains("bed")) {
                return "bedwars_two_one_duels";
            } else if (line.contains("rush") || line.contains("bedrush")) {
                return "bedwars_two_one_duels_rush";
            } else if (line.contains("quake") || line.contains("quakecraft")) {
                return "quake_duel";
            }
        }
        return null;
    }

    private static String cleanName(String name) {
        return name.replaceAll("§[0-9a-frk-o]", "").trim();
    }

    public static String stripColor(String input) {
        if (input == null)
            return "";
        return input.replaceAll("§.", "");
    }

    private static Set<String> loadHashesFromJson() {
        try {
            InputStream inputStream = Denicker.class.getResourceAsStream("/hashset.json");
            if (inputStream == null) {
                System.err.println("[S] hashset.json not found in resources");
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
