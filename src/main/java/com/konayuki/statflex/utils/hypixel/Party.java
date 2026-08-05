package com.konayuki.statflex.utils.hypixel;

import com.konayuki.statflex.utils.chat.Chat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public final class Party {

    public interface Callback {
        void onState(boolean inParty);
    }

    private static final Party INSTANCE = new Party();

    private static final long STATE_TTL_MS = 5 * 60 * 1000L;
    private static final long REQUEST_COOLDOWN_MS = 10 * 1000L;
    private static final int REQUEST_TIMEOUT_TICKS = 100;

    private volatile Boolean inParty;
    private volatile long updatedAt;

    private final List<Callback> waiting = new ArrayList<>();
    private boolean awaiting;
    private int waitTicks;
    private long requestedAt;

    private Party() {
    }

    public static Party get() {
        return INSTANCE;
    }

    public static void resolve(Callback callback) {
        if (callback == null) {
            return;
        }
        Chat.run(() -> INSTANCE.request(callback));
    }

    public static boolean isIn() {
        return Boolean.TRUE.equals(INSTANCE.inParty);
    }

    public static boolean isKnown() {
        return INSTANCE.inParty != null && !INSTANCE.isStale();
    }

    public static void forget() {
        INSTANCE.inParty = null;
        INSTANCE.updatedAt = 0;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) {
            return;
        }
        String line = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (line == null) {
            return;
        }
        line = line.trim();
        if (!line.isEmpty()) {
            observe(line);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !awaiting) {
            return;
        }
        if (++waitTicks >= REQUEST_TIMEOUT_TICKS) {
            finish(Boolean.TRUE.equals(inParty));
        }
    }

    private void request(Callback callback) {
        if (inParty != null && !isStale()) {
            callback.onState(inParty);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            callback.onState(false);
            return;
        }

        if (awaiting) {
            waiting.add(callback);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - requestedAt < REQUEST_COOLDOWN_MS) {
            callback.onState(Boolean.TRUE.equals(inParty));
            return;
        }

        waiting.add(callback);
        awaiting = true;
        waitTicks = 0;
        requestedAt = now;
        mc.thePlayer.sendChatMessage("/pl");
    }

    private void observe(String line) {
        String lower = line.toLowerCase();

        if (lower.startsWith("party members (")
                || lower.startsWith("party leader:")
                || lower.startsWith("party moderators:")
                || lower.startsWith("party members:")
                || lower.startsWith("you'll be partying with:")
                || (lower.startsWith("you have joined") && lower.contains("party!"))
                || (lower.endsWith("joined the party.") && !isChat(lower))) {
            set(true);
            return;
        }

        if (lower.startsWith("you are not currently in a party")
                || lower.startsWith("you are not in a party")
                || lower.startsWith("you left the party")
                || lower.startsWith("you have been kicked from the party")
                || lower.startsWith("you have been removed from the party")
                || lower.startsWith("you disbanded the party")
                || (lower.contains("has disbanded the party") && !isChat(lower))) {
            set(false);
        }
    }

    private void set(boolean state) {
        inParty = state;
        updatedAt = System.currentTimeMillis();
        if (awaiting) {
            finish(state);
        }
    }

    private void finish(boolean state) {
        awaiting = false;
        waitTicks = 0;
        if (waiting.isEmpty()) {
            return;
        }

        List<Callback> callbacks = new ArrayList<>(waiting);
        waiting.clear();
        for (Callback callback : callbacks) {
            try {
                callback.onState(state);
            } catch (Exception e) {
            }
        }
    }

    private boolean isStale() {
        return System.currentTimeMillis() - updatedAt > STATE_TTL_MS;
    }

    private static boolean isChat(String lower) {
        return lower.indexOf(':') >= 0;
    }

    private static String describe(Boolean state) {
        if (state == null) {
            return "unknown";
        }
        return state ? "in a party" : "not in a party";
    }
}
