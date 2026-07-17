package com.konayuki.statflex.features.skin;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.HttpSecureConnection;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Settings;
import com.konayuki.statflex.utils.Toggles;

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

    public static void savePlayerSkinAsync(String playerName, boolean useNpcSkin) {
        new Thread(() -> savePlayerSkin(playerName, useNpcSkin), "Skin-Thread").start();
    }

    private static void savePlayerSkin(String playerName, boolean useNpcSkin) {
        try {
            Minecraft mc = Minecraft.getMinecraft();

            if (useNpcSkin) {
                if (tryLocalSkinSave(playerName, mc)) {
                    return;
                }
            }

            Profile.PlayerInfo info = Profile.getPlayerInfo(playerName);
            if (info == null) {
                Chat.send("§8[§cS§8]§7 Player not found: " + playerName);
                return;
            }

            UUID uuid = parseUuid(info.uuid);
            String textureJson = fetchTextureJson(uuid);

            if (textureJson == null) {
                Chat.send("§8[§cS§8]§7 Failed to fetch skin data");
                return;
            }

            JsonObject root =
                    new JsonParser().parse(textureJson).getAsJsonObject();
            JsonObject textures = root.getAsJsonObject("textures");

            if (textures == null || !textures.has("SKIN")) {
                Chat.send("§8[§cS§8]§7 No skin found.");
                return;
            }

            String skinUrl =
                    textures.getAsJsonObject("SKIN")
                            .get("url").getAsString();

            downloadAndSaveSkin(skinUrl, playerName, uuid);

        } catch (Exception e) {
            e.printStackTrace();
            Chat.send(Messages.UNEXPECTED_ERROR);
        }
    }

    private static boolean tryLocalSkinSave(String playerName, Minecraft mc) {
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
                        downloadAndSaveSkin(skinUrl, playerName, uuid);
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

    private static String fetchTextureJson(UUID uuid) {
        try {
            String urlStr =
                    "https://sessionserver.mojang.com/session/minecraft/profile/"
                            + uuid.toString().replace("-", "")
                            + "?unsigned=false";

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (Toggles.ignoreCertificates && conn instanceof HttpsURLConnection) {
                HttpSecureConnection.trustAllCertificates((HttpsURLConnection) conn);
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

    private static void downloadAndSaveSkin(String urlStr, String name, UUID uuid) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (Toggles.ignoreCertificates && conn instanceof HttpsURLConnection) {
                HttpSecureConnection.trustAllCertificates((HttpsURLConnection) conn);
            }

            BufferedImage image;
            try (InputStream in = conn.getInputStream()) {
                image = ImageIO.read(in);
            }

            File downloadDir = Settings.getInstance().getSkinSaveDir();
            if (!downloadDir.exists()) downloadDir.mkdirs();

            File out =
                    new File(downloadDir, name + "_" + uuid + ".png");
            ImageIO.write(image, "png", out);

            sendClickablePath(out);

        } catch (Exception e) {
            e.printStackTrace();
            Chat.send("§8[§cS§8]§7 Failed to download skin");
        }
    }

    private static Property findLocalSkinProperty(String playerName) {
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

    private static void sendClickablePath(File file) {
        Minecraft mc = Minecraft.getMinecraft();

        mc.addScheduledTask(() -> {
            if (mc.thePlayer == null) return;

            ChatComponentText prefix =
                    new ChatComponentText("§8[§cS§8]§7 Saved skin: ");
            ChatComponentText path =
                    new ChatComponentText("§e" + file.getName());
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

    private static UUID parseUuid(String raw) {
        if (raw == null) throw new IllegalArgumentException("UUID is null");

        if (raw.length() == 32) {
            raw = raw.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        }

        return UUID.fromString(raw);
    }
}