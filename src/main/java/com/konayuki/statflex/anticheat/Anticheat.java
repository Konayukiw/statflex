package com.konayuki.statflex.anticheat;

import com.konayuki.statflex.anticheat.event.ReceivedPacketDetector;
import com.konayuki.statflex.client.ChatManager;
import com.konayuki.statflex.config.Settings;
import com.konayuki.statflex.system.Messages;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public final class Anticheat {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Anticheat INSTANCE = new Anticheat();

    private static final String AUTO_BLOCK = "Auto Block";
    private static final String NO_FALL = "NoFall";
    private static final String NO_SLOW = "NoSlow";
    private static final String SCAFFOLD = "Scaffold";
    private static final String LEGIT_SCAFFOLD = "Legit Scaffold";
    private static final String LAG_RANGE = "Lag Range";

    private static final int FREEZE_TICKS_REQUIRED = 2;
    private static final int MAX_SHORT_FREEZE_TICKS = 4;
    private static final int MIN_BURST_MOVE_PACKETS = 1;
    private static final int MAX_SHORT_BURST_MOVE_PACKETS = 6;
    private static final int LAG_RANGE_PATTERN_FLAGS_REQUIRED = 4;
    private static final int MAX_BURST_FROZEN_TICKS_SPREAD = 1;

    private static final long LAG_RANGE_PATTERN_WINDOW_MS = 1400L;
    private static final long WAITING_TIMEOUT_MS = 700L;
    private static final long FROZEN_TIMEOUT_MS = 900L;

    private static final double MIN_MOVEMENT_SPEED = 0.03D;
    private static final double SERVER_POS_EPSILON = 0.0015D;
    private static final double MIN_EXPECTED_MOVE_PER_FROZEN_TICK = 0.04D;
    private static final double MAX_EXPECTED_MOVE_PER_FROZEN_TICK = 0.6D;
    private static final double MAX_PRE_FREEZE_SPEED_VARIANCE = 0.0009D;
    private static final double NEARBY_ENEMY_RANGE = 15.0D;

    private static boolean registered;

    private final Map<UUID, Map<String, Long>> flags = new HashMap<>();
    private final Map<UUID, PlayerData> players = new HashMap<>();
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

            PlayerData data = players.get(player.getUniqueID());
            if (data == null) {
                data = new PlayerData();
                data.player = player;
            }

            data.update(player);
            performCheck(player, data);
            data.updateServerPos(player);
            data.updateSneak(player);
            players.put(player.getUniqueID(), data);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivedPacketDetector event) {
        lastClientBoundPacket = System.currentTimeMillis();

        Packet<?> packet = event.getPacket();

        if (packet instanceof S14PacketEntity) {
            if (mc.theWorld != null) {
                Entity entity = ((S14PacketEntity) packet).getEntity(mc.theWorld);
                if (entity instanceof EntityPlayer) {
                    PlayerData data = players.get(entity.getUniqueID());
                    if (data != null && data.lagRangeState == PlayerData.LagRangeState.FROZEN) {
                        data.movePacketsSinceFreeze++;
                        debug("[S] %s packet: %s (%d)", entity.getName(), packet.getClass().getSimpleName(), data.movePacketsSinceFreeze);
                    }
                }
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            if (mc.theWorld != null) {
                int entityId = ((S18PacketEntityTeleport) packet).getEntityId();
                Entity entity = mc.theWorld.getEntityByID(entityId);
                if (entity instanceof EntityPlayer) {
                    PlayerData data = players.get(entity.getUniqueID());
                    if (data != null && data.lagRangeState == PlayerData.LagRangeState.FROZEN) {
                        data.movePacketsSinceFreeze++;
                        debug("[S] %s packet: %s (%d)", entity.getName(), packet.getClass().getSimpleName(), data.movePacketsSinceFreeze);
                    }
                }
            }
        }
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

    private void performCheck(EntityPlayer player, PlayerData data) {
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

    private void checkLagRange(EntityPlayer player, PlayerData data) {
        long now = System.currentTimeMillis();
        double serverPosX = AnticheatUtils.getServerPosX(player);
        double serverPosZ = AnticheatUtils.getServerPosZ(player);

        if (!isLagRangeEligible(player) || !hasNearbyEnemy(player) || !isCombatLike(player, data)) {
            resetLagRangeTracking(data);
            return;
        }

        if (Double.isNaN(serverPosX) || Double.isNaN(serverPosZ)
                || Double.isNaN(data.serverPosX) || Double.isNaN(data.serverPosZ)) {
            return;
        }

        boolean isMoving = data.speed > MIN_MOVEMENT_SPEED;
        boolean serverPosChanged = Math.abs(data.serverPosX - serverPosX) > SERVER_POS_EPSILON
                || Math.abs(data.serverPosZ - serverPosZ) > SERVER_POS_EPSILON;
        boolean packetFrozen = isMoving && !serverPosChanged;
        boolean continuingPattern = data.lagRangePatternVl > 0
                && now - data.lastLagRangeBurstTime <= LAG_RANGE_PATTERN_WINDOW_MS;

        if (data.lagRangePatternVl > 0 && !continuingPattern) {
            data.lagRangePatternVl = 0;
            data.lastLagRangeBurstTime = 0L;
            data.resetBurstHistory();
        }

        switch (data.lagRangeState) {
            case IDLE:
                if (packetFrozen) {
                    data.lagRangeState = PlayerData.LagRangeState.WAITING_FREEZE;
                    data.lagRangeStateEnteredAt = now;
                    data.freezeCandidateTicks = 1;
                    data.consecutiveFrozenTicks = 0;
                    data.burstHadRealMove = false;
                    debug("[S] %s IDLE > WAITING_FREEZE (candidate=1)", player.getName());
                }
                break;

            case WAITING_FREEZE:
                if (now - data.lagRangeStateEnteredAt > WAITING_TIMEOUT_MS) {
                    debug("[S] %s WAITING_FREEZE timeout", player.getName());
                    resetLagRangeState(data);
                    break;
                }

                if (packetFrozen) {
                    data.freezeCandidateTicks++;
                    data.consecutiveFrozenTicks++;
                    if (isMoving) data.burstHadRealMove = true;

                    if (data.freezeCandidateTicks >= FREEZE_TICKS_REQUIRED) {
                        data.lagRangeState = PlayerData.LagRangeState.FROZEN;
                        data.lagRangeStateEnteredAt = now;
                        data.movePacketsSinceFreeze = 0;
                        data.freezeStartServerPosX = data.serverPosX;
                        data.freezeStartServerPosZ = data.serverPosZ;
                        data.preFreezeAverageSpeed = data.getAverageSpeed();
                        debug("[S] %s WAITING->FROZEN (freezeCandidate=%d preAvg=%.5f)", player.getName(), data.freezeCandidateTicks, data.preFreezeAverageSpeed);
                    }
                } else if (serverPosChanged) {
                    debug("[S] %s WAITING cancelled (frozenTicks=%d)", player.getName(), data.consecutiveFrozenTicks);
                    resetLagRangeState(data);
                }
                break;

            case FROZEN:
                if (now - data.lagRangeStateEnteredAt > FROZEN_TIMEOUT_MS) {
                    debug("[S] %s FROZEN timeout", player.getName());
                    resetLagRangeTracking(data);
                    break;
                }

                if (packetFrozen) {
                    data.consecutiveFrozenTicks++;
                    if (isMoving) data.burstHadRealMove = true;
                } else if (serverPosChanged) {
                    debug("[S] %s FROZEN -> serverPosChanged (frozenTicks=%d movePackets=%d)", player.getName(), data.consecutiveFrozenTicks, data.movePacketsSinceFreeze);

                    boolean shortFreeze = data.consecutiveFrozenTicks >= FREEZE_TICKS_REQUIRED
                            && data.consecutiveFrozenTicks <= MAX_SHORT_FREEZE_TICKS;

                    boolean burstPackets = data.movePacketsSinceFreeze >= MIN_BURST_MOVE_PACKETS
                            && data.movePacketsSinceFreeze <= MAX_SHORT_BURST_MOVE_PACKETS;

                    double preFreezeVariance = data.computePreFreezeSpeedVariance();
                    boolean preFreezeStable = preFreezeVariance <= MAX_PRE_FREEZE_SPEED_VARIANCE;

                    double dx = serverPosX - data.freezeStartServerPosX;
                    double dz = serverPosZ - data.freezeStartServerPosZ;
                    double burstDistance = Math.sqrt(dx * dx + dz * dz);
                    double movePerFrozenTick = Double.NaN;
                    boolean validMoveRatio = false;

                    if (data.consecutiveFrozenTicks > 0) {
                        movePerFrozenTick = burstDistance / (double) data.consecutiveFrozenTicks;
                        validMoveRatio = movePerFrozenTick >= MIN_EXPECTED_MOVE_PER_FROZEN_TICK
                                && movePerFrozenTick <= MAX_EXPECTED_MOVE_PER_FROZEN_TICK;
                    }

                    debug("[S] %s Burst: dist=%.4f perTick=%.4f preVar=%.6f preAvg=%.5f", player.getName(), burstDistance, movePerFrozenTick, preFreezeVariance, data.preFreezeAverageSpeed);

                    boolean burstDistanceReasonable = false;
                    if (!Double.isNaN(data.preFreezeAverageSpeed)) {
                        double expectedMin = data.preFreezeAverageSpeed * data.consecutiveFrozenTicks * 0.9;
                        double expectedMax = Math.max(expectedMin, data.preFreezeAverageSpeed * data.consecutiveFrozenTicks * 1.6);
                        burstDistanceReasonable = burstDistance >= expectedMin && burstDistance <= expectedMax;
                    } else {
                        burstDistanceReasonable = validMoveRatio;
                    }

                    if (data.burstHadRealMove && shortFreeze && burstPackets && (validMoveRatio || burstDistanceReasonable) && preFreezeStable) {
                        if (data.lastLagRangeBurstTime == 0L || now - data.lastLagRangeBurstTime <= LAG_RANGE_PATTERN_WINDOW_MS) {
                            data.lagRangePatternVl++;
                        } else {
                            data.lagRangePatternVl = 1;
                            data.resetBurstHistory();
                        }
                        data.lastLagRangeBurstTime = now;
                        data.pushBurstFrozenTicks(data.consecutiveFrozenTicks);

                        int spread = data.computeBurstFrozenTicksSpread();
                        debug("[S] %s LagRange pattern: VL=%d Spread=%s", player.getName(), data.lagRangePatternVl, spread == Integer.MAX_VALUE ? "n/a" : String.valueOf(spread));

                        if (data.lagRangePatternVl >= LAG_RANGE_PATTERN_FLAGS_REQUIRED && spread != Integer.MAX_VALUE && spread <= MAX_BURST_FROZEN_TICKS_SPREAD) {
                            alert(player, LAG_RANGE);
                        }
                    } else {
                        data.lagRangePatternVl = 0;
                        data.lastLagRangeBurstTime = 0L;
                        data.resetBurstHistory();
                    }

                    resetLagRangeState(data);
                }
                break;
        }

        data.serverPosX = serverPosX;
        data.serverPosZ = serverPosZ;
    }

    private static void resetLagRangeTracking(PlayerData data) {
        resetLagRangeState(data);
        data.lagRangePatternVl = 0;
        data.lastLagRangeBurstTime = 0L;
        data.resetBurstHistory();
    }

    private static void resetLagRangeState(PlayerData data) {
        data.lagRangeState = PlayerData.LagRangeState.IDLE;
        data.lagRangeStateEnteredAt = 0L;
        data.consecutiveFrozenTicks = 0;
        data.movePacketsSinceFreeze = 0;
        data.burstHadRealMove = false;
        data.freezeStartServerPosX = Double.NaN;
        data.freezeStartServerPosZ = Double.NaN;
        data.freezeCandidateTicks = 0;
        data.preFreezeAverageSpeed = Double.NaN;
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
        if (world == null) return false;

        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.getEntityBoundingBox().expand(NEARBY_ENEMY_RANGE, NEARBY_ENEMY_RANGE, NEARBY_ENEMY_RANGE)
        );

        for (EntityLivingBase entity : nearby) {
            if (entity == player) continue;
            if (entity instanceof EntityPlayer) {
                double distSq = player.getDistanceSqToEntity(entity);
                if (distSq <= NEARBY_ENEMY_RANGE * NEARBY_ENEMY_RANGE) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCombatLike(EntityPlayer player, PlayerData data) {
        if (data.speedHistoryFilled < PlayerData.SPEED_HISTORY_SIZE) return false;

        if (data.getAverageSpeed() < MIN_MOVEMENT_SPEED) return false;

        if (!hasNearbyEnemy(player)) return false;

        double var = data.computePreFreezeSpeedVariance();
        if (var > MAX_PRE_FREEZE_SPEED_VARIANCE) return false;

        if (data.fastTick < 2) return false;

        return true;
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
        ChatManager.send(Messages.PREFIX + "§e" + displayName + " §7flagged §c" + cheat);
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

    private void debug(String fmt, Object... args) {
        try {
            System.out.printf(fmt + "%n", args);
        } catch (Throwable ignored) {}
    }
}
