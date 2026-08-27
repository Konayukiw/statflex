package com.konayuki.statflex.features.denick;

import com.konayuki.statflex.features.bedwars.BedwarsList;
import com.konayuki.statflex.features.duels.Duels;
import com.konayuki.statflex.features.skywars.Skywars;
import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Toggle;
import com.konayuki.statflex.utils.api.Profile;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.utils.chat.Warn;
import com.konayuki.statflex.utils.hypixel.Party;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Denick {

    private final Set<String> parsed = new HashSet<>();

    private static final Set<String> nickedPlayers = ConcurrentHashMap.newKeySet();

    private static final Set<String> reported = ConcurrentHashMap.newKeySet();

    private static final Gson gson = new Gson();

    private static final Set<String> nicks = hashes();

    private volatile String gameType;
    private volatile String gameMode;
    private boolean locrawSent;
    private final List<String> awaitingGame = new ArrayList<>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!Toggle.isDenick()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) {
            return;
        }

        for (NetworkPlayerInfo npi : mc.getNetHandler().getPlayerInfoMap()) {
            if (npi == null || npi.getGameProfile() == null) {
                continue;
            }

            String name = npi.getGameProfile().getName();
            if (name == null || name.isEmpty() || parsed.contains(name)) {
                continue;
            }

            if (judge(npi, name)) {
                parsed.add(name);
            }
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.entity != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        parsed.clear();
        reported.clear();
        clear();
        gameType = null;
        gameMode = null;
        locrawSent = false;
        awaitingGame.clear();
    }

    public static void mark(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        nickedPlayers.add(name.toLowerCase());
    }

    public static void clear() {
        nickedPlayers.clear();
    }

    private boolean judge(NetworkPlayerInfo npi, String name) {
        GameProfile profile = npi.getGameProfile();

        if (isNpc(npi, name)) {
            return true;
        }

        if (npi.getResponseTime() <= 0) {
            return false;
        }

        if (profile.getId() == null || profile.getId().version() == 2) {
            return true;
        }

        boolean judged = false;
        for (Property property : profile.getProperties().get("textures")) {
            try {
                JsonObject texture = decode(property.getValue());
                if (texture == null) {
                    continue;
                }
                String hash = hash(texture);
                if (hash == null) {
                    continue;
                }

                judged = true;
                String owner = texture.has("profileName") ? texture.get("profileName").getAsString() : "";
                Debug.log("Denick " + name + ": listed=" + nicks.contains(hash)
                        + ", profileName=" + (owner.isEmpty() ? "<none>" : owner)
                        + ", hash=" + hash);

                if (nicks.contains(hash)) {
                    confirm(name);
                    return true;
                }

                if (!owner.isEmpty() && !owner.equalsIgnoreCase(name)) {
                    reveal(owner, name);
                }
                return true;
            } catch (Exception e) {
                judged = true;
            }
        }

        return judged;
    }

    private void reveal(String owner, String nick) {
        mark(nick);
        if (!reported.add(nick.toLowerCase())) {
            return;
        }

        Chat.send(Messages.PREFIX + Color.RED + owner + Color.GRAY + " is nicked as "
                + Color.RED + nick + Color.GRAY + "!");
        warn(owner + " is nicked as " + nick + "!");
        stats(owner);
    }

    private void confirm(String nick) {
        new Thread(() -> {
            Boolean exists = Profile.exists(nick);
            if (Boolean.FALSE.equals(exists)) {
                report(nick);
            }
        }, "Denick").start();
    }

    private void report(String nick) {
        mark(nick);
        if (!reported.add(nick.toLowerCase())) {
            return;
        }

        Chat.send(Messages.PREFIX + "Found a nicked player: " + Color.RED + nick);
        warn("Nicked a nicked player: " + nick);
    }

    private void warn(String report) {
        Party.resolve(inParty -> {
            if (!inParty) {
                return;
            }
            new Thread(() -> Warn.warn(report), "Denick").start();
        });
    }

    private void stats(String realName) {
        String type = gameType;
        if (type != null) {
            dispatch(realName, type, gameMode);
            return;
        }

        awaitingGame.add(realName);
        if (locrawSent) {
            return;
        }
        locrawSent = true;

        Locraw.get().request(new Locraw.LocrawCallback() {
            @Override
            public void onReceived(String type, String mode) {
                gameType = type;
                gameMode = mode;
                locrawSent = type != null;
                flush(type, mode);
            }

            @Override
            public void onTimeout() {
                locrawSent = false;
                awaitingGame.clear();
                Debug.log("Denick: /locraw timed out.");
            }
        });
    }

    private void flush(String type, String mode) {
        List<String> names = new ArrayList<>(awaitingGame);
        awaitingGame.clear();
        for (String name : names) {
            dispatch(name, type, mode);
        }
    }

    private void dispatch(String profileName, String gameType, String mode) {
        if (gameType == null) {
            Chat.send(Messages.UNKNOWN_GAMEMODE);
            return;
        }

        switch (gameType) {
            case "BEDWARS":
                BedwarsList.list(Arrays.asList(profileName), true);
                break;
            case "DUELS":
                Duels.stats(profileName, Duels.detect(mode == null ? null : mode.toLowerCase()), true);
                break;
            case "SKYWARS":
                Skywars.stats(profileName, null);
                break;
            default:
                Chat.send(Messages.UNKNOWN_GAMEMODE);
        }
    }

    private static boolean isNpc(NetworkPlayerInfo npi, String name) {
        IChatComponent displayName = npi.getDisplayName();
        String shown = displayName != null ? displayName.getFormattedText() : name;
        return clean(shown).replaceAll("\\s+", "").contains("[NPC]") || name.contains("[NPC]");
    }

    public static boolean isNick(String name) {
        return name != null && nickedPlayers.contains(name.toLowerCase());
    }

    private static JsonObject decode(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        return gson.fromJson(decoded, JsonObject.class);
    }

    private static String hash(JsonObject texture) {
        if (!texture.has("textures")) {
            return null;
        }
        JsonObject textures = texture.getAsJsonObject("textures");

        if (!textures.has("SKIN")) {
            return null;
        }
        JsonObject skin = textures.getAsJsonObject("SKIN");

        if (!skin.has("url")) {
            return null;
        }
        String[] parts = skin.get("url").getAsString().split("/");
        return parts.length > 4 ? parts[4] : null;
    }

    private static String clean(String name) {
        return name.replaceAll("§[0-9a-frk-o]", "").trim();
    }

    private static Set<String> hashes() {
        try {
            InputStream inputStream = Denick.class.getResourceAsStream("/hashset.json");
            if (inputStream == null) {
                Debug.error("hashset.json not found in resources");
                return new HashSet<>();
            }
            List<String> hashList = gson.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                    new TypeToken<List<String>>() {
                    }.getType());
            return new HashSet<>(hashList);
        } catch (Exception e) {
            Debug.error("Failed to load hashset.json: " + e);
            return new HashSet<>();
        }
    }
}
