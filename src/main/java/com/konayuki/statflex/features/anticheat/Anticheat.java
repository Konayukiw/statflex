package com.konayuki.statflex.features.anticheat;

import com.konayuki.statflex.utils.event.ReceivedPacketDetector;
import com.konayuki.statflex.utils.Chat;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.World;
import net.minecraft.util.IChatComponent;
import com.mojang.authlib.GameProfile;

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
    private static final String LAG_RANGE = "Suspicious Lag Range";

    private static final double NEARBY_ENEMY_RANGE = 15.0D;

    private static boolean registered;

    private final Map<UUID, Map<String, Long>> flags = new HashMap<>();
    private final Map<UUID, com.konayuki.statflex.utils.PlayerUtils> players = new ConcurrentHashMap<UUID, com.konayuki.statflex.utils.PlayerUtils>();
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
        if (event.phase != TickEvent.Phase.END || !AnticheatUtils.nullCheck() || mc.isSingleplayer()) {
            return;
        }

        if (mc.theWorld == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isCheckTarget(player)) continue;

            com.konayuki.statflex.utils.PlayerUtils data = getData(player);

            data.update(player);
            performCheck(player, data);
            data.updateServerPos(player);
            data.updateSneak(player);

            updatePlayerData(player, data);
            checkLagRange(player, data);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivedPacketDetector event) {
        lastClientBoundPacket = System.currentTimeMillis();
        trackMovementPacket(event.getPacket());
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

    private com.konayuki.statflex.utils.PlayerUtils getData(EntityPlayer player) {
        com.konayuki.statflex.utils.PlayerUtils data = players.get(player.getUniqueID());
        if (data == null) {
            data = new com.konayuki.statflex.utils.PlayerUtils();
            data.player = player;
            players.put(player.getUniqueID(), data);
        }
        return data;
    }

    private void updatePlayerData(EntityPlayer player, com.konayuki.statflex.utils.PlayerUtils data) {
        data.ticksExisted = player.ticksExisted;
        double dx = player.posX - data.posX;
        double dz = player.posZ - data.posZ;

        data.speed = Math.sqrt(dx * dx + dz * dz);

        data.posX = player.posX;
        data.posY = player.posY;
        data.posZ = player.posZ;
        
        data.speedHistory[data.speedHistoryIndex] = data.speed;
        data.speedHistoryIndex = (data.speedHistoryIndex + 1) % com.konayuki.statflex.utils.PlayerUtils.SPEED_HISTORY_SIZE;
        if (data.speedHistoryFilled < com.konayuki.statflex.utils.PlayerUtils.SPEED_HISTORY_SIZE) {
            data.speedHistoryFilled++;
        }
    }

    private void updateServerPos(EntityPlayer player, com.konayuki.statflex.utils.PlayerUtils data) {
        data.serverPosX = AnticheatUtils.getServerPosX(player);
        data.serverPosY = AnticheatUtils.getServerPosY(player);
        data.serverPosZ = AnticheatUtils.getServerPosZ(player);
    }

    private void updateSneak(EntityPlayer player, com.konayuki.statflex.utils.PlayerUtils data) {
        if (player.isSneaking()) {
            if (!data.sneaking) {
                data.sneaking = true;
                data.lastSneakTick = player.ticksExisted;
            }
            data.sneakTicks++;
        } else {
            data.sneaking = false;
            data.sneakTicks = 0;
        }
    }

    private void performCheck(EntityPlayer player, com.konayuki.statflex.utils.PlayerUtils data) {
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
                if (!(AnticheatUtils.getBlock(blockPos) instanceof net.minecraft.block.BlockAir)) {
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
                && AnticheatUtils.timeBetween(System.currentTimeMillis(), lastClientBoundPacket) <= 200L) {

            double serverPosX = AnticheatUtils.getServerPosX(player);
            double serverPosY = AnticheatUtils.getServerPosY(player);
            double serverPosZ = AnticheatUtils.getServerPosZ(player);

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
                    && !AnticheatUtils.overVoid(serverPosX, serverPosY, serverPosZ)
                    && AnticheatUtils.distanceToGround(player) > 3.0D
                    && !AnticheatUtils.onLadder(player)
                    && !player.isInWater()
                    && !player.isInLava()) {
                alert(player, NO_FALL);
            }

            checkLagRange(player, data);
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

    private void checkLagRange(EntityPlayer player, com.konayuki.statflex.utils.PlayerUtils data) {
        if (!isLagRangeEligible(player)) {
            return;
        }

        long now = System.currentTimeMillis();
        decayLagRangeVl(data, now);

        int currentPackets = data.movementPacketsThisTick;

        boolean hasEnemy = hasNearbyEnemy(player);
        if (hasEnemy) {
            updateApproachState(player, data);
            updateFreezeState(data, currentPackets);
        } else {
            data.approaching = false;
            data.consecutiveZeroTicks = 0;
        }

        updateWaitingForBurst(data);

        boolean swingStarted = player.isSwingInProgress && !data.wasSwinging;
        data.wasSwinging = player.isSwingInProgress;

        boolean burst = currentPackets >= com.konayuki.statflex.utils.PlayerUtils.MIN_BURST_PACKETS;

        Debug.log("%s waiting=%s waitTicks=%d zeroTicks=%d packets=%d approaching=%s swing=%s vl=%d",
                player.getName(), data.waitingForBurst, data.waitingTicks,
                data.consecutiveZeroTicks, currentPackets, data.approaching,
                player.isSwingInProgress, data.lagRangeVl);

        if (data.waitingForBurst
                && burst
                && (swingStarted || player.isSwingInProgress)) {

            data.lagRangeVl++;
            data.lastVlIncreaseAt = now;
            data.waitingForBurst = false;
            data.consecutiveZeroTicks = 0;

            Debug.log("%s VL++ to %d (burst=%d packets, swingStarted=%s)",
                    player.getName(), data.lagRangeVl, currentPackets, swingStarted);

            if (data.lagRangeVl >= com.konayuki.statflex.utils.PlayerUtils.LAG_RANGE_VL_ALERT) {
                alert(player, LAG_RANGE);
                data.lagRangeVl = 0;
            }
        }

        data.movementPacketsThisTick = 0;
    }

    private void updateApproachState(EntityPlayer player, com.konayuki.statflex.utils.PlayerUtils data) {
        double distanceSq = player.getDistanceSqToEntity(mc.thePlayer);

        if (Double.isNaN(data.lastDistanceSq)) {
            data.lastDistanceSq = distanceSq;
            data.approaching = false;
            return;
        }

        data.approaching = distanceSq < data.lastDistanceSq;

        data.lastDistanceSq = distanceSq;
    }

    private void updateFreezeState(com.konayuki.statflex.utils.PlayerUtils data, int currentPackets) {
        if (data.approaching && currentPackets == 0) {
            data.consecutiveZeroTicks++;
        } else {
            data.consecutiveZeroTicks = 0;
        }

        if (data.consecutiveZeroTicks >= com.konayuki.statflex.utils.PlayerUtils.FREEZE_TICKS_THRESHOLD) {
            data.waitingForBurst = true;
            data.waitingTicks = com.konayuki.statflex.utils.PlayerUtils.BURST_WINDOW_TICKS;
        }
    }

    private void updateWaitingForBurst(com.konayuki.statflex.utils.PlayerUtils data) {
        if (!data.waitingForBurst) {
            return;
        }
        data.waitingTicks--;
        if (data.waitingTicks <= 0) {
            data.waitingForBurst = false;
        }
    }

    private void decayLagRangeVl(com.konayuki.statflex.utils.PlayerUtils data, long now) {
        if (data.lagRangeVl <= 0) {
            return;
        }
        long elapsed = now - data.lastVlIncreaseAt;
        if (elapsed <= 0) {
            return;
        }
        int steps = (int) (elapsed / com.konayuki.statflex.utils.PlayerUtils.VL_DECAY_STEP_MS);
        if (steps > 0) {
            data.lagRangeVl = Math.max(0, data.lagRangeVl - steps);
            data.lastVlIncreaseAt += steps * com.konayuki.statflex.utils.PlayerUtils.VL_DECAY_STEP_MS;
        }
    }

    private boolean isLagRangeEligible(EntityPlayer player) {
        return player.ticksExisted >= 60
                && !player.isInWater()
                && !player.isInLava()
                && !player.isRiding()
                && !AnticheatUtils.onLadder(player);
    }

    @SuppressWarnings("unchecked")
    private boolean hasNearbyEnemy(EntityPlayer player) {
        World world = player.worldObj;
        if (world == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<EntityPlayer> nearby = world.getEntitiesWithinAABB(
                EntityPlayer.class,
                player.getEntityBoundingBox().expand(
                        NEARBY_ENEMY_RANGE,
                        NEARBY_ENEMY_RANGE,
                        NEARBY_ENEMY_RANGE)
        );

        Team playerTeam = player.getTeam();

        for (EntityPlayer other : nearby) {

            if (other == player)
                continue;

            if (other.isDead)
                continue;

            if (player.getDistanceSqToEntity(other)
                    > NEARBY_ENEMY_RANGE * NEARBY_ENEMY_RANGE)
                continue;

            if (mc.getNetHandler() == null)
                continue;

            NetworkPlayerInfo info =
                    mc.getNetHandler().getPlayerInfo(other.getUniqueID());

            if (info == null)
                continue;

            GameProfile profile = info.getGameProfile();

            if (profile == null)
                continue;

            String name = profile.getName();

            IChatComponent display = info.getDisplayName();

            String clean = cleanName(
                    display != null
                            ? display.getFormattedText()
                            : name)
                    .trim()
                    .replaceAll("\\s+", "");

            Team otherTeam = other.getTeam();

            if (profile.getId() != null && profile.getId().version() == 2)
                continue;

            if (clean.contains("[NPC]") || name.contains("[NPC]"))
                continue;

            if (info.getResponseTime() <= 0)
                continue;

            if (playerTeam != null
                    && otherTeam != null
                    && playerTeam.isSameTeam(otherTeam))
                continue;

            return true;
        }

        return false;
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
                if (previous != null && AnticheatUtils.timeBetween(previous.longValue(), now) <= (long) (interval * 1000.0D)) {
                    return;
                }
            }
            playerFlags.put(cheat, now);
            flags.put(player.getUniqueID(), playerFlags);
        }

        String displayName = player.getDisplayName() == null ? player.getName() : player.getDisplayName().getFormattedText();
        Chat.send(Messages.PREFIX + "§e" + displayName + " §7flagged §c" + cheat);
        if (AnticheatUtils.timeBetween(lastAlert, now) >= 1500L) {
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

    private static String cleanName(String name) {
        return name.replaceAll("§[0-9a-frk-o]", "").trim();
    }

    private void trackMovementPacket(Packet packet) {
        Debug.log(packet.getClass().getSimpleName());
        if (!(packet instanceof S14PacketEntity
                || packet instanceof S14PacketEntity.S15PacketEntityRelMove
                || packet instanceof S14PacketEntity.S16PacketEntityLook
                || packet instanceof S14PacketEntity.S17PacketEntityLookMove
                || packet instanceof S19PacketEntityHeadLook)) {
            return;
        }

        int entityId = getPacketEntityId(packet);
        if (entityId == -1) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getEntityId() == entityId) {
                com.konayuki.statflex.utils.PlayerUtils data = getData(player);
                data.movementPacketsThisTick++;
                break;
            }
        }
    }

    private int getPacketEntityId(Packet packet) {
        try {
            Field field = packet.getClass().getDeclaredField("entityId");
            field.setAccessible(true);

            int entityId = field.getInt(packet);
            Debug.log("EntityID: %d", entityId);

            return entityId;
        } catch (NoSuchFieldException e) {
            try {
                Field field = packet.getClass().getDeclaredField("field_145963_a");
                field.setAccessible(true);

                int entityId = field.getInt(packet);
                Debug.log("EntityID: %d", entityId);

                return entityId;
            } catch (Exception ignored) {
                return -1;
            }
        } catch (Exception ignored) {
            return -1;
        }
    }
}
