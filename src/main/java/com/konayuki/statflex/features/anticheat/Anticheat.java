package com.konayuki.statflex.features.anticheat;

import com.konayuki.statflex.utils.*;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.packet.ReceivedPacketDetector;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Anticheat {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Anticheat INSTANCE = new Anticheat();

    private static final String AUTO_BLOCK = "Auto Block";
    private static final String NO_FALL = "NoFall";
    private static final String NO_SLOW = "NoSlow";
    private static final String SCAFFOLD = "Scaffold";
    private static final String LEGIT_SCAFFOLD = "Legit Scaffold";

    private static boolean registered;

    private final Map<UUID, Map<String, Long>> flags = new HashMap<>();
    private final Map<UUID, AnticheatUtil> players = new ConcurrentHashMap<UUID, AnticheatUtil>();
    private long lastAlert;
    private long lastClientBoundPacket;

    private Anticheat() {}

    public static void register() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(INSTANCE);
            registered = true;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !AnticheatUtil.nullCheck() || mc.isSingleplayer()) {
            return;
        }

        if (mc.theWorld == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isCheckTarget(player)) continue;

            AnticheatUtil data = getData(player);

            data.update(player);
            performCheck(player, data);
            data.updateServerPos(player);
            data.updateSneak(player);

            updatePlayerData(player, data);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivedPacketDetector event) {
        lastClientBoundPacket = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.entity == mc.thePlayer) reset();
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        reset();
    }

    private boolean isCheckTarget(EntityPlayer player) {
        return player != null
                && player != mc.thePlayer
                && !player.isDead
                && player.getName() != null
                && !player.getName().isEmpty();
    }

    private AnticheatUtil getData(EntityPlayer player) {
        AnticheatUtil data = players.get(player.getUniqueID());
        if (data == null) {
            data = new AnticheatUtil();
            data.player = player;
            players.put(player.getUniqueID(), data);
        }
        return data;
    }

    private void updatePlayerData(EntityPlayer player, AnticheatUtil data) {
        data.ticksExisted = player.ticksExisted;
        double dx = player.posX - data.posX;
        double dz = player.posZ - data.posZ;

        data.speed = Math.sqrt(dx * dx + dz * dz);

        data.posX = player.posX;
        data.posY = player.posY;
        data.posZ = player.posZ;
        
        data.speedHistory[data.speedHistoryIndex] = data.speed;
        data.speedHistoryIndex = (data.speedHistoryIndex + 1) % AnticheatUtil.SPEED_HISTORY_SIZE;
        if (data.speedHistoryFilled < AnticheatUtil.SPEED_HISTORY_SIZE) {
            data.speedHistoryFilled++;
        }
    }

    private void performCheck(EntityPlayer player, AnticheatUtil data) {
        if (data.autoBlockTicks >= 10) {
            alert(player, AUTO_BLOCK);
            return;
        }

        if (data.sneakTicks >= 3) {
            alert(player, LEGIT_SCAFFOLD);
            return;
        }

        if (data.noSlowTicks == 11 && data.speed >= 0.08D) {
            alert(player, NO_SLOW);
            return;
        }

        if (player.isSwingInProgress
                && player.rotationPitch >= 70.0F
                && player.getHeldItem() != null
                && player.getHeldItem().getItem() instanceof net.minecraft.item.ItemBlock
                && data.fastTick >= 20
                && player.ticksExisted - data.lastSneakTick >= 30
                && player.ticksExisted - data.aboveVoidTicks >= 20) {

            net.minecraft.util.BlockPos blockPos = player.getPosition().down(2);
            boolean overAir = true;
            for (int i = 0; i < 4; i++) {
                if (!(AnticheatUtil.getBlock(blockPos) instanceof net.minecraft.block.BlockAir)) {
                    overAir = false;
                    break;
                }
                blockPos = blockPos.down();
            }
            if (overAir) {
                alert(player, SCAFFOLD);
                return;
            }
        }

        if (!player.capabilities.disableDamage
                && AnticheatUtil.timeBetween(System.currentTimeMillis(), lastClientBoundPacket) <= 200L) {

            double serverPosX = AnticheatUtil.getServerPosX(player);
            double serverPosY = AnticheatUtil.getServerPosY(player);
            double serverPosZ = AnticheatUtil.getServerPosZ(player);

            if (Double.isNaN(serverPosX) || Double.isNaN(serverPosY) || Double.isNaN(serverPosZ)
                    || Double.isNaN(data.serverPosX) || Double.isNaN(data.serverPosY) || Double.isNaN(data.serverPosZ)) {
                return;
            }

            double deltaX = Math.abs(data.serverPosX - serverPosX);
            double deltaY = data.serverPosY - serverPosY;
            double deltaZ = Math.abs(data.serverPosZ - serverPosZ);

            if (deltaY >= 5.0D
                    && deltaX <= 10.0D
                    && deltaZ <= 10.0D
                    && deltaY <= 40.0D
                    && !AnticheatUtil.overVoid(serverPosX, serverPosY, serverPosZ)
                    && AnticheatUtil.distanceToGround(player) > 3.0D
                    && !AnticheatUtil.onLadder(player)
                    && !player.isInWater()
                    && !player.isInLava()) {
                alert(player, NO_FALL);
            }
        }
    }

    private double getFlagIntervalSeconds() {
        return clampFlagInterval(readFlagInterval());
    }

    private double readFlagInterval() {
        try {
            Field field = Settings.class.getDeclaredField("flagInterval");
            field.setAccessible(true);
            Object owner = Modifier.isStatic(field.getModifiers()) ? null : findSettingsInstance();
            return toDouble(field.get(owner), 5.0D);
        } catch (Throwable ignored) {
            return 5.0D;
        }
    }

    private Object findSettingsInstance() {
        try {
            Field instance = Settings.class.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (Throwable ignored) {}
        try {
            Field instance = Settings.class.getDeclaredField("instance");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (Throwable ignored) {}
        try {
            Method getInstance = Settings.class.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            return getInstance.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private double clampFlagInterval(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 20.0D;
        if (value < 0.0D) return 0.0D;
        if (value > 20.0D) return 20.0D;
        return value;
    }

    private void alert(EntityPlayer player, String cheat) {
        long now = System.currentTimeMillis();
        double interval = getFlagIntervalSeconds();

        if (interval > 0.0D) {
            Map<String, Long> playerFlags = flags.get(player.getUniqueID());
            if (playerFlags == null) {
                playerFlags = new HashMap<>();
            } else {
                Long previous = playerFlags.get(cheat);
                if (previous != null && AnticheatUtil.timeBetween(previous.longValue(), now) <= (long) (interval * 1000.0D)) {
                    return;
                }
            }
            playerFlags.put(cheat, now);
            flags.put(player.getUniqueID(), playerFlags);
        }

        String displayName = player.getDisplayName() == null ? player.getName() : player.getDisplayName().getFormattedText();
        Chat.send(Messages.PREFIX + "§e" + displayName + " §7flagged §c" + cheat);
        if (AnticheatUtil.timeBetween(lastAlert, now) >= 1500L) {
            mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
            lastAlert = now;
        }
    }

    private void reset() {
        players.clear();
        flags.clear();
        lastAlert = 0L;
        lastClientBoundPacket = 0L;
    }

    private double toDouble(Object value, double fallback) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); } catch (NumberFormatException ignored) { return fallback; }
        }
        if (value == null) return fallback;
        String[] methodNames = new String[] {"getInput", "getValue", "get", "doubleValue", "floatValue"};
        for (String methodName : methodNames) {
            try {
                Method method = value.getClass().getMethod(methodName);
                method.setAccessible(true);
                Object result = method.invoke(value);
                if (result instanceof Number) return ((Number) result).doubleValue();
            } catch (Throwable ignored) {}
        }
        return fallback;
    }
}
