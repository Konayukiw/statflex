package com.konayuki.statflex.features.rpc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.Setting;
import com.konayuki.statflex.utils.Toggle;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class DiscordRPC {

    private static DiscordRPC instance;

    private static final int VERSION = 1;
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;
    private static final int FORCE_REFRESH_TICKS = 20 * 60 * 5;
    private static final int MAX_DRAIN_FRAMES = 16;

    private static final String[] PIPE_NAMES = {
            "\\\\.\\pipe\\discord-ipc-0",
            "\\\\.\\pipe\\discord-ipc-1",
            "\\\\.\\pipe\\discord-ipc-2",
            "\\\\.\\pipe\\discord-ipc-3",
            "\\\\.\\pipe\\discord-ipc-4",
            "\\\\.\\pipe\\discord-ipc-5",
            "\\\\.\\pipe\\discord-ipc-6",
            "\\\\.\\pipe\\discord-ipc-7",
            "\\\\.\\pipe\\discord-ipc-8",
            "\\\\.\\pipe\\discord-ipc-9",
    };

    private final JsonParser jsonParser = new JsonParser();

    private RandomAccessFile pipe;
    private boolean connected = false;
    private int updateTickCounter = 0;
    private int forceRefreshCounter = 0;
    private String lastServerIP = "";
    private String lastPlayerName = "";
    private long sessionStartSeconds = 0;

    private DiscordRPC() {
    }

    public static synchronized DiscordRPC getInstance() {
        if (instance == null) {
            instance = new DiscordRPC();
        }
        return instance;
    }

    public void onTick() {
        if (!Toggle.discordRpc) {
            if (connected) {
                disconnect();
            }
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return;
        }

        updateTickCounter++;
        if (updateTickCounter < 100) {
            return;
        }
        updateTickCounter = 0;
        forceRefreshCounter += 100;

        String playerName = mc.thePlayer.getName();
        String serverIP;

        if (mc.isSingleplayer()) {
            serverIP = "Singleplayer";
        } else if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            serverIP = mc.getCurrentServerData().serverIP;
        } else {
            serverIP = "Unknown";
        }

        boolean needUpdate = !connected
                || !playerName.equals(lastPlayerName)
                || !serverIP.equals(lastServerIP)
                || forceRefreshCounter >= FORCE_REFRESH_TICKS;

        if (needUpdate) {
            if (updatePresence(playerName, serverIP)) {
                lastPlayerName = playerName;
                lastServerIP = serverIP;
                forceRefreshCounter = 0;
            }
        }
    }

    public void connect() {
        if (connected) {
            return;
        }

        String appId = Setting.getInstance().discordRpcApplicationId;
        if (appId == null || appId.isEmpty()) {
            return;
        }
        appId = appId.trim();
        if (!appId.matches("\\d{17,20}")) {
            return;
        }

        for (String pipeName : PIPE_NAMES) {
            try {
                pipe = new RandomAccessFile(pipeName, "rw");
                if (performHandshake(appId)) {
                    connected = true;
                    return;
                }
                closePipeQuietly();
            } catch (Exception e) {
                closePipeQuietly();
            }
        }
    }

    public void disconnect() {
        if (!connected && pipe == null) {
            return;
        }
        try {
            if (pipe != null && connected) {
                try {
                    send(OP_FRAME, buildSetActivityJson(null));
                } catch (Exception ignored) {
                }
            }
        } finally {
            closePipeQuietly();
            connected = false;
            lastServerIP = "";
            lastPlayerName = "";
            sessionStartSeconds = 0;
            forceRefreshCounter = 0;
        }
    }

    public boolean updatePresence(String playerName, String serverIP) {
        if (!Toggle.discordRpc) {
            return false;
        }

        if (!connected) {
            connect();
            if (!connected) {
                return false;
            }
        }

        String details = "Playing " + serverIP;
        String state = playerName;
        String largeImageKey = "statflex";
        String largeImageText = "Minecraft 1.8.9";

        if (sessionStartSeconds <= 0) {
            sessionStartSeconds = System.currentTimeMillis() / 1000L;
        }

        JsonObject activity = new JsonObject();
        activity.addProperty("details", details);
        activity.addProperty("state", state);
        activity.addProperty("instance", false);

        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", largeImageKey);
        assets.addProperty("large_text", largeImageText);
        activity.add("assets", assets);

        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", sessionStartSeconds);
        activity.add("timestamps", timestamps);

        String nonce = UUID.randomUUID().toString();
        String payload = buildSetActivityJson(activity, nonce);

        try {
            send(OP_FRAME, payload);
            if (!awaitCommandResult(nonce)) {
                markBroken();
                return false;
            }
            return true;
        } catch (IOException e) {
            markBroken();
            return false;
        }
    }

    private boolean performHandshake(String appId) throws IOException {
        JsonObject handshake = new JsonObject();
        handshake.addProperty("v", VERSION);
        handshake.addProperty("client_id", appId);
        send(OP_HANDSHAKE, handshake.toString());

        for (int i = 0; i < MAX_DRAIN_FRAMES; i++) {
            Frame frame = readFrame();
            if (frame == null) {
                return false;
            }

            if (frame.opcode == OP_CLOSE) {
                return false;
            }

            if (frame.opcode == OP_PING) {
                send(OP_PONG, frame.json);
                continue;
            }

            if (frame.opcode != OP_FRAME) {
                continue;
            }

            JsonObject body = parseJson(frame.json);
            if (body == null) {
                continue;
            }

            String evt = body.has("evt") && !body.get("evt").isJsonNull()
                    ? body.get("evt").getAsString()
                    : null;
            String cmd = body.has("cmd") && !body.get("cmd").isJsonNull()
                    ? body.get("cmd").getAsString()
                    : null;

            if ("ERROR".equals(evt)) {
                return false;
            }

            if ("READY".equals(evt) || ("DISPATCH".equals(cmd) && "READY".equals(evt))) {
                return true;
            }
        }
        return false;
    }

    private boolean awaitCommandResult(String nonce) throws IOException {
        for (int i = 0; i < MAX_DRAIN_FRAMES; i++) {
            Frame frame = readFrame();
            if (frame == null) {
                return false;
            }

            if (frame.opcode == OP_CLOSE) {
                return false;
            }

            if (frame.opcode == OP_PING) {
                send(OP_PONG, frame.json);
                continue;
            }

            if (frame.opcode != OP_FRAME) {
                continue;
            }

            JsonObject body = parseJson(frame.json);
            if (body == null) {
                continue;
            }

            String frameNonce = body.has("nonce") && !body.get("nonce").isJsonNull()
                    ? body.get("nonce").getAsString()
                    : null;
            String evt = body.has("evt") && !body.get("evt").isJsonNull()
                    ? body.get("evt").getAsString()
                    : null;
            String cmd = body.has("cmd") && !body.get("cmd").isJsonNull()
                    ? body.get("cmd").getAsString()
                    : null;

            if ("DISPATCH".equals(cmd) && frameNonce == null) {
                continue;
            }

            if (nonce != null && frameNonce != null && !nonce.equals(frameNonce)) {
                if ("ERROR".equals(evt)) {
                }
                continue;
            }

            if ("ERROR".equals(evt)) {
                return false;
            }

            if ("SET_ACTIVITY".equals(cmd) || frameNonce != null) {
                return true;
            }
        }
        return false;
    }

    private String buildSetActivityJson(JsonObject activity) {
        return buildSetActivityJson(activity, UUID.randomUUID().toString());
    }

    private String buildSetActivityJson(JsonObject activity, String nonce) {
        JsonObject root = new JsonObject();
        root.addProperty("cmd", "SET_ACTIVITY");
        root.addProperty("nonce", nonce);

        JsonObject args = new JsonObject();
        args.addProperty("pid", getPid());
        if (activity == null) {
            args.add("activity", com.google.gson.JsonNull.INSTANCE);
        } else {
            args.add("activity", activity);
        }
        root.add("args", args);
        return root.toString();
    }

    private void send(int opcode, String json) throws IOException {
        if (pipe == null) {
            throw new IOException("pipe is null");
        }
        byte[] jsonBytes = json.getBytes("UTF-8");
        ByteBuffer header = ByteBuffer.allocate(8);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(jsonBytes.length);

        pipe.write(header.array());
        pipe.write(jsonBytes);
    }

    private Frame readFrame() throws IOException {
        if (pipe == null) {
            return null;
        }

        byte[] headerBytes = new byte[8];
        try {
            pipe.readFully(headerBytes);
        } catch (IOException e) {
            return null;
        }

        ByteBuffer header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = header.getInt();
        int length = header.getInt();

        if (length < 0 || length > 1_000_000) {
            return null;
        }

        byte[] body = new byte[length];
        if (length > 0) {
            pipe.readFully(body);
        }
        String json = new String(body, "UTF-8");
        return new Frame(opcode, json);
    }

    private JsonObject parseJson(String json) {
        try {
            return jsonParser.parse(json).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private void markBroken() {
        connected = false;
        closePipeQuietly();
    }

    private void closePipeQuietly() {
        if (pipe != null) {
            try {
                pipe.close();
            } catch (Exception ignored) {
            }
            pipe = null;
        }
    }

    private static int getPid() {
        try {
            String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            int at = name.indexOf('@');
            if (at > 0) {
                return Integer.parseInt(name.substring(0, at));
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }

    private static final class Frame {
        final int opcode;
        final String json;

        Frame(int opcode, String json) {
            this.opcode = opcode;
            this.json = json;
        }
    }
}