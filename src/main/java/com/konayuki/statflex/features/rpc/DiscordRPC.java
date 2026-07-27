package com.konayuki.statflex.features.rpc;

import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Settings;
import com.konayuki.statflex.utils.Toggles;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class DiscordRPC {

    private static DiscordRPC instance;

    private static final int VERSION = 1;
    private static final int HANDSHAKE = 0;
    private static final int FRAME = 1;

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

    private RandomAccessFile pipe;
    private boolean connected = false;
    private int updateTickCounter = 0;
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
        if (!Toggles.discordRpc) {
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
                || !serverIP.equals(lastServerIP);

        if (needUpdate) {
            lastPlayerName = playerName;
            lastServerIP = serverIP;
            updatePresence(playerName, serverIP);
        }
    }

    public void connect() {
        if (connected) {
            return;
        }

        String appId = Settings.getInstance().discordRpcApplicationId;
        if (appId == null || appId.isEmpty()) {
            Debug.log("[S] Discord RPC: no Application ID configured");
            return;
        }

        for (String pipeName : PIPE_NAMES) {
            try {
                pipe = new RandomAccessFile(pipeName, "rw");
                sendHandshake(appId);
                connected = true;
                Debug.log("[S] Discord RPC connected via " + pipeName);
                return;
            } catch (Exception e) {
            }
        }

        Debug.log("[S] Discord RPC: could not connect to any pipe");
    }

    public void disconnect() {
        if (!connected) {
            return;
        }
        try {
            if (pipe != null) {
                pipe.close();
            }
        } catch (Exception ignored) {
        }
        pipe = null;
        connected = false;
        lastServerIP = "";
        lastPlayerName = "";
        sessionStartSeconds = 0;
        Debug.log("[S] Discord RPC disconnected.");
    }

    private void sendHandshake(String appId) throws IOException {
        String json = "{\"v\":" + VERSION + ",\"client_id\":\"" + appId + "\"}";
        send(HANDSHAKE, json);
    }

    public void updatePresence(String playerName, String serverIP) {
        if (!Toggles.discordRpc) {
            return;
        }

        if (!connected) {
            connect();
            if (!connected) {
                return;
            }
        }

        String details = "Playing " + serverIP;
        String state = playerName;
        String largeImageKey = "minecraft";
        String largeImageText = "Minecraft 1.8.9";

        if (sessionStartSeconds <= 0) {
            sessionStartSeconds = System.currentTimeMillis() / 1000L;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":")
                .append(getPid())
                .append(",\"activity\":{");
        sb.append("\"details\":").append(quote(details)).append(",");
        sb.append("\"state\":").append(quote(state)).append(",");
        sb.append("\"assets\":{");
        sb.append("\"large_image\":").append(quote(largeImageKey)).append(",");
        sb.append("\"large_text\":").append(quote(largeImageText));
        sb.append("},");
        sb.append("\"timestamps\":{\"start\":").append(sessionStartSeconds).append("}");
        sb.append("}},\"nonce\":\"").append(System.nanoTime()).append("\"}");

        try {
            send(FRAME, sb.toString());
        } catch (IOException e) {
            Debug.log("[S] Discord RPC failed to send: " + e.getMessage());
            connected = false;
            try {
                if (pipe != null) {
                    pipe.close();
                }
            } catch (Exception ignored) {
            }
            pipe = null;
        }
    }

    private void send(int opcode, String json) throws IOException {
        byte[] jsonBytes = json.getBytes("UTF-8");
        ByteBuffer header = ByteBuffer.allocate(8);
        header.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(jsonBytes.length);

        pipe.write(header.array());
        pipe.write(jsonBytes);
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

    private static String quote(String s) {
        if (s == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
