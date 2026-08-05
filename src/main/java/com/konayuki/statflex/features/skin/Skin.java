package com.konayuki.statflex.features.skin;

import com.konayuki.statflex.utils.*;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.Toggle;

import com.google.gson.*;

import com.konayuki.statflex.utils.api.Profile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.client.network.NetworkPlayerInfo;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

public class Skin {
    public static void save(String playerName, boolean useNpcSkin) {
        new Thread(() -> fetch(playerName, useNpcSkin), "Skin").start();
    }

    private static void fetch(String playerName, boolean useNpcSkin) {
        try {
            Minecraft mc = Minecraft.getMinecraft();

            if (useNpcSkin) {
                if (fromTab(playerName, mc)) {
                    return;
                }
            }

            Profile.PlayerInfo info = Profile.info(playerName);
            if (info == null) {
                Chat.send(Messages.PREFIX + "Player not found: " + playerName);
                return;
            }

            UUID uuid = uuid(info.uuid);
            String textureJson = texture(uuid);

            if (textureJson == null) {
                Chat.send(Messages.PREFIX + "Failed to fetch skin data");
                return;
            }

            JsonObject root =
                    new JsonParser().parse(textureJson).getAsJsonObject();
            JsonObject textures = root.getAsJsonObject("textures");

            if (textures == null || !textures.has("SKIN")) {
                Chat.send(Messages.PREFIX + "No skin found.");
                return;
            }

            String skinUrl =
                    textures.getAsJsonObject("SKIN")
                            .get("url").getAsString();

            download(skinUrl, playerName, uuid);

        } catch (Exception e) {
            e.printStackTrace();
            Chat.send(Messages.UNEXPECTED_ERROR);
        }
    }

    private static boolean fromTab(String playerName, Minecraft mc) {
        try {
            NetworkPlayerInfo localInfo = null;

            for (NetworkPlayerInfo npi : mc.getNetHandler().getPlayerInfoMap()) {
                if (npi.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                    localInfo = npi;
                    break;
                }
            }

            if (localInfo != null) {
                GameProfile profile = localInfo.getGameProfile();
                Collection<Property> props = profile.getProperties().get("textures");

                if (props != null && !props.isEmpty()) {
                    Property texturesProp = props.iterator().next();

                    String decoded = new String(
                            Base64.getDecoder().decode(texturesProp.getValue()),
                            StandardCharsets.UTF_8
                    );

                    JsonObject root =
                            new JsonParser().parse(decoded).getAsJsonObject();
                    JsonObject textures = root.getAsJsonObject("textures");

                    if (textures != null && textures.has("SKIN")) {
                        String skinUrl =
                                textures.getAsJsonObject("SKIN")
                                        .get("url").getAsString();

                        UUID uuid = profile.getId();
                        download(skinUrl, playerName, uuid);
                        return true;
                    }
                }
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String texture(UUID uuid) {
        try {
            String urlStr =
                    "https://sessionserver.mojang.com/session/minecraft/profile/"
                            + uuid.toString().replace("-", "")
                            + "?unsigned=false";

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (Toggle.ignoreCertificates && conn instanceof HttpsURLConnection) {
                Connection.trustAll((HttpsURLConnection) conn);
            }

            if (conn.getResponseCode() != 200) {
                return null;
            }

            JsonObject root;
            try (InputStreamReader reader =
                         new InputStreamReader(conn.getInputStream())) {
                root = new JsonParser().parse(reader).getAsJsonObject();
            }

            JsonArray props = root.getAsJsonArray("properties");
            if (props == null) return null;

            for (JsonElement e : props) {
                JsonObject prop = e.getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String value = prop.get("value").getAsString();
                    return new String(Base64.getDecoder().decode(value));
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void download(String urlStr, String name, UUID uuid) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

            if (Toggle.ignoreCertificates && conn instanceof HttpsURLConnection) {
                Connection.trustAll((HttpsURLConnection) conn);
            }

            BufferedImage image;
            try (InputStream in = conn.getInputStream()) {
                image = ImageIO.read(in);
            }

            File downloadDir = Setting.get().dir();
            if (!downloadDir.exists()) downloadDir.mkdirs();

            File out =
                    new File(downloadDir, name + "_" + uuid + ".png");
            ImageIO.write(image, "png", out);

            announce(out);

        } catch (Exception e) {
            e.printStackTrace();
            Chat.send(Messages.PREFIX + "Failed to download skin");
        }
    }

    private static void announce(File file) {
        Minecraft mc = Minecraft.getMinecraft();

        mc.addScheduledTask(() -> {
            if (mc.thePlayer == null) return;

            ChatComponentText prefix =
                    new ChatComponentText(Messages.PREFIX + "Saved skin: ");
            ChatComponentText path =
                    new ChatComponentText(Color.YELLOW + file.getName());
            path.getChatStyle()
                    .setChatClickEvent(
                            new net.minecraft.event.ClickEvent(
                                    net.minecraft.event.ClickEvent.Action.OPEN_FILE,
                                    file.getParentFile().getAbsolutePath()
                            )
                    )
                    .setUnderlined(true);

            prefix.appendSibling(path);
            mc.thePlayer.addChatMessage(prefix);
        });
    }

    private static UUID uuid(String raw) {
        if (raw == null) throw new IllegalArgumentException("UUID is null");

        if (raw.length() == 32) {
            raw = raw.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        }

        return UUID.fromString(raw);
    }

    private static Property property(String playerName) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.getNetHandler() == null) return null;

        for (NetworkPlayerInfo npi : mc.getNetHandler().getPlayerInfoMap()) {
            GameProfile profile = npi.getGameProfile();
            if (profile == null || profile.getName() == null) continue;

            if (profile.getName().equalsIgnoreCase(playerName)) {
                return profile.getProperties().get("textures").iterator().hasNext()
                        ? profile.getProperties().get("textures").iterator().next()
                        : null;
            }
        }
        return null;
    }
}