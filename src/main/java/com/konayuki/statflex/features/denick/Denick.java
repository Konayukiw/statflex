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

/**
 * Spots nicked players in the tab list.
 *
 * <p>Server forwards a signed skin blob for every entry, and that blob still names
 * the account the skin belongs to. So a nick lands in one of two states:
 *
 * <ul>
 *   <li>the blob names somebody other than the tab name, which denicks the player;</li>
 *   <li>the blob names nobody and the skin comes from Server's nick pool, which only
 *       says a nick is present.</li>
 * </ul>
 *
 * <p>Either way the nick name is recorded through {@link #check(String)} so the
 * stat lists skip it instead of reporting it as a failed lookup, it is announced
 * locally, and it is reported in party chat when we are in a party. Only a denicked
 * player gets a stat lookup, since only then is there an account to look up.
 */
public class Denick {

    /** Tab entries already judged in this world; cleared whenever the world changes. */
    private final Set<String> parsed = new HashSet<>();

    /** Names known to be nicks, so stat lookups skip them instead of failing on them. */
    private static final Set<String> nickedPlayers = ConcurrentHashMap.newKeySet();

    /** Nicks already announced, so the same one is not reported twice in a world. */
    private static final Set<String> reported = ConcurrentHashMap.newKeySet();

    private static final Gson gson = new Gson();

    /** The skin hashes Server hands out to nicked players. */
    private static final Set<String> nicks = loadHashesFromJson();

    /** Every nick found in one world is in the same game, so /locraw is asked once. */
    private volatile String gameType;
    private volatile String gameMode;
    private boolean locrawSent;
    private final List<String> awaitingGame = new ArrayList<>();

    /** Records a name as a nick so stat lookups can skip it. */
    public static void markNicked(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        nickedPlayers.add(name.toLowerCase());
    }

    /**
     * True when the name belongs to a nick we spotted. Server has no profile for a
     * nick, so a lookup would only fail: callers use this to drop the name quietly.
     */
    public static boolean check(String name) {
        return name != null && nickedPlayers.contains(name.toLowerCase());
    }

    public static void clearNicked() {
        nickedPlayers.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!Toggle.isDenickEnabled()) {
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

            if (check(npi, name)) {
                parsed.add(name);
            }
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent event) {
        if (event.entity != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        parsed.clear();
        reported.clear();
        clearNicked();
        gameType = null;
        gameMode = null;
        locrawSent = false;
        awaitingGame.clear();
    }

    private boolean check(NetworkPlayerInfo npi, String name) {
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
                String hash = skinHash(texture);
                if (hash == null) {
                    continue;
                }

                judged = true;
                String owner = texture.has("profileName") ? texture.get("profileName").getAsString() : "";
                Debug.log("Denick " + name + ": listed=" + nicks.contains(hash)
                        + ", profileName=" + (owner.isEmpty() ? "<none>" : owner)
                        + ", hash=" + hash);

                if (!owner.isEmpty()) {
                    if (!owner.equalsIgnoreCase(name)) {
                        denicked(owner, name);
                    }
                    return true;
                }

                if (nicks.contains(hash)) {
                    confirmNicked(name);
                }
                return true;
            } catch (Exception e) {
                judged = true;
            }
        }

        return judged;
    }

    private void denicked(String owner, String nick) {
        markNicked(nick);
        if (!reported.add(nick.toLowerCase())) {
            return;
        }

        Chat.send(Messages.PREFIX + Color.RED + owner + Color.GRAY + " is nicked as "
                + Color.RED + nick + Color.GRAY + "!");
        warn(owner + " is nicked as " + nick + "!");
        requestStats(owner);
    }

    private void confirmNicked(String nick) {
        new Thread(() -> {
            Boolean exists = Profile.exists(nick);
            if (Boolean.FALSE.equals(exists)) {
                nicked(nick);
            }
        }, "Denick").start();
    }

    private void nicked(String nick) {
        markNicked(nick);
        if (!reported.add(nick.toLowerCase())) {
            return;
        }

        Chat.send(Messages.PREFIX + "Found a nicked player: " + Color.RED + nick);
        warn("Nicked player found: " + nick);
    }

    private void warn(String report) {
        Party.resolve(inParty -> {
            if (!inParty) {
                return;
            }
            new Thread(() -> Warn.warn(report), "Denick").start();
        });
    }

    private void requestStats(String realName) {
        String type = gameType;
        if (type != null) {
            processStats(realName, type, gameMode);
            return;
        }

        awaitingGame.add(realName);
        if (locrawSent) {
            return;
        }
        locrawSent = true;

        Locraw.getInstance().sendLocraw(new Locraw.LocrawCallback() {
            @Override
            public void onLocrawReceived(String type, String mode) {
                gameType = type;
                gameMode = mode;
                locrawSent = type != null;
                flushAwaitingGame(type, mode);
            }

            @Override
            public void onLocrawTimeout() {
                locrawSent = false;
                awaitingGame.clear();
                Debug.log("Denick: /locraw timed out.");
            }
        });
    }

    private void flushAwaitingGame(String type, String mode) {
        List<String> names = new ArrayList<>(awaitingGame);
        awaitingGame.clear();
        for (String name : names) {
            processStats(name, type, mode);
        }
    }

    private void processStats(String profileName, String gameType, String mode) {
        if (gameType == null) {
            Chat.send(Messages.UNKNOWN_GAMEMODE);
            return;
        }

        switch (gameType) {
            case "BEDWARS":
                BedwarsList.listBedwarsStats(Arrays.asList(profileName), true);
                break;
            case "DUELS":
                Duels.fetchStats(profileName, Duels.detectMode(mode == null ? null : mode.toLowerCase()), true);
                break;
            case "SKYWARS":
                Skywars.fetchStats(profileName, null);
                break;
            default:
                Chat.send(Messages.UNKNOWN_GAMEMODE);
        }
    }

    private static boolean isNpc(NetworkPlayerInfo npi, String name) {
        IChatComponent displayName = npi.getDisplayName();
        String shown = displayName != null ? displayName.getFormattedText() : name;
        return cleanName(shown).replaceAll("\\s+", "").contains("[NPC]") || name.contains("[NPC]");
    }

    private static JsonObject decode(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        return gson.fromJson(decoded, JsonObject.class);
    }

    private static String skinHash(JsonObject texture) {
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
