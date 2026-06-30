package com.konayuki.statflex.anticheat;

import com.konayuki.statflex.anticheat.event.SentPacketDetector;
import com.konayuki.statflex.system.Messages;
import com.konayuki.statflex.config.Settings;
import com.konayuki.statflex.anticheat.event.ReceivedPacketDetector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.world.World;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public final class Anticheat {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Anticheat INSTANCE = new Anticheat();

    private static final String AUTO_BLOCK = "Auto Block";
    private static final String NO_FALL = "NoFall";
    private static final String NO_SLOW = "NoSlow";
    private static final String SCAFFOLD = "Scaffold";
    private static final String LEGIT_SCAFFOLD = "Legit Scaffold";
    private static final String LAG_RANGE = "Lag Range";

    private static final int FREEZE_TICKS_REQUIRED   = 2;
    private static final int MAX_SHORT_FREEZE_TICKS = 3;
    private static final int MIN_BURST_MOVE_PACKETS = 1;
    private static final int MAX_SHORT_BURST_MOVE_PACKETS = 3;
    private static final int LAG_RANGE_PATTERN_FLAGS_REQUIRED = 4;
    private static final long LAG_RANGE_PATTERN_WINDOW_MS = 1200L;
    private static final double MIN_MOVEMENT_SPEED = 0.03D;
    private static final double SERVER_POS_EPSILON = 0.001D;
    private static final double NEARBY_ENEMY_RANGE = 15.0D;

    private static final double MAX_PRE_FREEZE_SPEED_VARIANCE = 0.0006D;

    private static final double MIN_EXPECTED_MOVE_PER_FROZEN_TICK = 0.05D;
    private static final double MAX_EXPECTED_MOVE_PER_FROZEN_TICK = 0.45D;

    private static final int MAX_BURST_FROZEN_TICKS_SPREAD = 1;

    private static boolean registered;

    private final Map<UUID, Map<String, Long>> flags = new HashMap<UUID, Map<String, Long>>();
    private final Map<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();
    private long lastAlert;
    private long lastClientBoundPacket;

    private Anticheat() {
    }

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

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isCheckTarget(player)) {
                continue;
            }

            PlayerData data = players.get(player.getUniqueID());
            if (data == null) {
                data = new PlayerData();
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

                    if (data != null) {
                        if (data.lagRangeState == PlayerData.LagRangeState.FROZEN) {

                            data.movePacketsSinceFreeze++;

                            System.out.printf(
                                    "[S] %s packet: %s (%d)%n",
                                    entity.getName(),
                                    packet.getClass().getSimpleName(),
                                    data.movePacketsSinceFreeze
                            );
                        }
                    }
                }
            }

        } else if (packet instanceof S18PacketEntityTeleport) {

            if (mc.theWorld != null) {
                int entityId = ((S18PacketEntityTeleport) packet).getEntityId();
                Entity entity = mc.theWorld.getEntityByID(entityId);

                if (entity instanceof EntityPlayer) {
                    PlayerData data = players.get(entity.getUniqueID());

                    if (data != null) {
                        if (data.lagRangeState == PlayerData.LagRangeState.FROZEN) {

                            data.movePacketsSinceFreeze++;

                            System.out.printf(
                                    "[S] %s packet: %s (%d)%n",
                                    entity.getName(),
                                    packet.getClass().getSimpleName(),
                                    data.movePacketsSinceFreeze
                            );
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SentPacketDetector event) {
        Packet<?> packet = event.getPacket();

        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity use = (C02PacketUseEntity) packet;

            if (use.getAction() == C02PacketUseEntity.Action.ATTACK && mc.theWorld != null) {

                Entity entity = use.getEntityFromWorld(mc.theWorld);

                if (entity instanceof EntityPlayer) {
                    PlayerData data = players.get(entity.getUniqueID());

                    if (data != null) {
                        data.lastAttackPacketTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.entity == mc.thePlayer) {
            reset();
        }
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
                && player.getHeldItem().getItem() instanceof ItemBlock
                && data.fastTick >= 20
                && player.ticksExisted - data.lastSneakTick >= 30
                && player.ticksExisted - data.aboveVoidTicks >= 20) {
            BlockPos blockPos = player.getPosition().down(2);
            boolean overAir = true;

            for (int i = 0; i < 4; i++) {
                if (!(AnticheatUtils.getBlock(blockPos) instanceof BlockAir)) {
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
                && AnticheatUtils.timeBetween(System.currentTimeMillis(), lastClientBoundPacket) <= 150L) {
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
        } catch (Throwable ignored) {
        }

        try {
            Field instance = Settings.class.getDeclaredField("instance");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (Throwable ignored) {
        }

        try {
            Method getInstance = Settings.class.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            return getInstance.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private double clampFlagInterval(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 20.0D;
        }

        if (value < 0.0D) {
            return 0.0D;
        }

        if (value > 20.0D) {
            return 20.0D;
        }

        return value;
    }

    private void checkLagRange(EntityPlayer player, PlayerData data) {
        if (!isLagRangeEligible(player) || !hasNearbyEnemy(player)) {
            resetLagRangeTracking(data);
            return;
        }

        long now = System.currentTimeMillis();
        double serverPosX = AnticheatUtils.getServerPosX(player);
        double serverPosZ = AnticheatUtils.getServerPosZ(player);

        if (Double.isNaN(serverPosX) || Double.isNaN(serverPosZ)
                || Double.isNaN(data.serverPosX) || Double.isNaN(data.serverPosZ)) {
            return;
        }

        boolean isMoving        = data.speed > MIN_MOVEMENT_SPEED;
        boolean serverPosChanged =
                Math.abs(data.serverPosX - serverPosX) > SERVER_POS_EPSILON
                        || Math.abs(data.serverPosZ - serverPosZ) > SERVER_POS_EPSILON;
        boolean packetFrozen    = isMoving && !serverPosChanged;
        boolean recentAttack = data.lastAttackPacketTime != Long.MIN_VALUE
                && now - data.lastAttackPacketTime < 300;
        boolean continuingPattern = data.lagRangePatternVl > 0
                && now - data.lastLagRangeBurstTime <= LAG_RANGE_PATTERN_WINDOW_MS;

        if (data.lagRangePatternVl > 0 && !continuingPattern) {
            data.lagRangePatternVl = 0;
            data.lastLagRangeBurstTime = 0L;
            data.resetBurstHistory();
        }

        switch (data.lagRangeState) {

            case IDLE:
                if (recentAttack || continuingPattern) {
                    double preFreezeVariance = data.computePreFreezeSpeedVariance();
                    if (preFreezeVariance > MAX_PRE_FREEZE_SPEED_VARIANCE) {
                        System.out.printf(
                                "[S] %s is a lagger: (variance=%.6f)%n",
                                player.getName(),
                                preFreezeVariance
                        );
                        break;
                    }

                    System.out.printf(
                            "[S] %s detected suspicious LagRange: Attack=%b VL=%d Variance=%.6f%n",
                            player.getName(),
                            recentAttack,
                            data.lagRangePatternVl,
                            preFreezeVariance
                    );
                    data.lagRangeState = PlayerData.LagRangeState.WAITING_FREEZE;
                    data.lagRangeStateEnteredAt = now;
                    data.consecutiveFrozenTicks = 0;
                    data.burstHadRealMove = false;
                }
                break;

            case WAITING_FREEZE:
                if (now - data.lagRangeStateEnteredAt > 500) {
                    resetLagRangeState(data);
                    break;
                }

                if (packetFrozen) {
                    data.consecutiveFrozenTicks++;
                    if (isMoving) data.burstHadRealMove = true;

                    if (data.consecutiveFrozenTicks >= FREEZE_TICKS_REQUIRED) {

                        data.lagRangeState = PlayerData.LagRangeState.FROZEN;
                        data.lagRangeStateEnteredAt = now;

                        data.movePacketsSinceFreeze = 0;
                        data.freezeStartServerPosX = data.serverPosX;
                        data.freezeStartServerPosZ = data.serverPosZ;

                        System.out.printf(
                                "[S] %s freezing: (%d ticks)%n",
                                player.getName(),
                                data.consecutiveFrozenTicks
                        );
                    }
                } else if (serverPosChanged) {
                    System.out.printf(
                            "[S] %s cancelled waiting: %d frozen%n",
                            player.getName(),
                            data.consecutiveFrozenTicks
                    );
                    resetLagRangeState(data);
                }
                break;

            case FROZEN:
                if (now - data.lagRangeStateEnteredAt > 600) {
                    resetLagRangeTracking(data);
                    break;
                }

                if (packetFrozen) {
                    data.consecutiveFrozenTicks++;
                    if (isMoving) data.burstHadRealMove = true;

                } else if (serverPosChanged) {
                    System.out.printf(
                            "[S] %s cancelled waiting: %d frozen%n",
                            player.getName(),
                            data.consecutiveFrozenTicks
                    );

                    boolean shortFreeze =
                            data.consecutiveFrozenTicks >= FREEZE_TICKS_REQUIRED
                                    && data.consecutiveFrozenTicks <= MAX_SHORT_FREEZE_TICKS;
                    boolean burstPackets =
                            data.movePacketsSinceFreeze >= MIN_BURST_MOVE_PACKETS
                                    && data.movePacketsSinceFreeze <= MAX_SHORT_BURST_MOVE_PACKETS;
                    boolean validMoveRatio = false;
                    double movePerFrozenTick = 0.0D;
                    if (!Double.isNaN(data.freezeStartServerPosX) && !Double.isNaN(data.freezeStartServerPosZ)
                            && data.consecutiveFrozenTicks > 0) {
                        double dx = serverPosX - data.freezeStartServerPosX;
                        double dz = serverPosZ - data.freezeStartServerPosZ;
                        double burstDistance = Math.sqrt(dx * dx + dz * dz);
                        movePerFrozenTick = burstDistance / (double) data.consecutiveFrozenTicks;
                        validMoveRatio = movePerFrozenTick >= MIN_EXPECTED_MOVE_PER_FROZEN_TICK
                                && movePerFrozenTick <= MAX_EXPECTED_MOVE_PER_FROZEN_TICK;
                    }

                    System.out.printf(
                            "[S] %s leaving freeze: "
                                    + "FrozenTicks=%d "
                                    + "MovePackets=%d "
                                    + "BurstMove=%b "
                                    + "ShortFreeze=%b "
                                    + "Attack=%b "
                                    + "MovePerFrozenTick=%.4f "
                                    + "ValidMoveRatio=%b%n",

                            player.getName(),
                            data.consecutiveFrozenTicks,
                            data.movePacketsSinceFreeze,
                            data.burstHadRealMove,
                            shortFreeze,
                            recentAttack,
                            movePerFrozenTick,
                            validMoveRatio
                    );

                    if (data.burstHadRealMove && shortFreeze && burstPackets && validMoveRatio) {
                        if (data.lastLagRangeBurstTime == 0L
                                || now - data.lastLagRangeBurstTime <= LAG_RANGE_PATTERN_WINDOW_MS) {
                            data.lagRangePatternVl++;
                        } else {
                            data.lagRangePatternVl = 1;
                            data.resetBurstHistory();
                        }

                        data.lastLagRangeBurstTime = now;

                        data.pushBurstFrozenTicks(data.consecutiveFrozenTicks);
                        int spread = data.computeBurstFrozenTicksSpread();

                        System.out.printf(
                                "[S] %s LagRange pattern: "
                                        + "VL=%d Frozen=%d MovePackets=%d Spread=%s%n",
                                player.getName(),
                                data.lagRangePatternVl,
                                data.consecutiveFrozenTicks,
                                data.movePacketsSinceFreeze,
                                spread == Integer.MAX_VALUE ? "n/a" : String.valueOf(spread)
                        );

                        if (data.lagRangePatternVl >= LAG_RANGE_PATTERN_FLAGS_REQUIRED
                                && spread != Integer.MAX_VALUE
                                && spread <= MAX_BURST_FROZEN_TICKS_SPREAD) {
                            alert(player, LAG_RANGE);

                            System.out.printf(
                                    "[S] %s flagged LagRange: "
                                            + "VL=%d Frozen=%d MovePackets=%d Spread=%d%n",
                                    player.getName(),
                                    data.lagRangePatternVl,
                                    data.consecutiveFrozenTicks,
                                    data.movePacketsSinceFreeze,
                                    spread
                            );
                        }
                    } else if (!shortFreeze || data.movePacketsSinceFreeze > MAX_SHORT_BURST_MOVE_PACKETS
                            || !validMoveRatio) {
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
        data.lastHorizontalSpeed = data.speed;
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
                player.getEntityBoundingBox().expand(
                        NEARBY_ENEMY_RANGE, NEARBY_ENEMY_RANGE, NEARBY_ENEMY_RANGE)
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


    private void alert(EntityPlayer player, String cheat) {
        long now = System.currentTimeMillis();
        double interval = getFlagIntervalSeconds();

        if (interval > 0.0D) {
            Map<String, Long> playerFlags = flags.get(player.getUniqueID());
            if (playerFlags == null) {
                playerFlags = new HashMap<String, Long>();
            } else {
                Long previous = playerFlags.get(cheat);
                if (previous != null && AnticheatUtils.timeBetween(previous.longValue(), now) <= (long) (interval * 1000.0D)) {
                    return;
                }
            }

            playerFlags.put(cheat, Long.valueOf(now));
            flags.put(player.getUniqueID(), playerFlags);
        }

        String displayName = player.getDisplayName() == null ? player.getName() : player.getDisplayName().getFormattedText();
        ChatComponentText message = new ChatComponentText(Messages.PREFIX + "§e" + displayName + " §7flagged §c" + cheat);
        mc.thePlayer.addChatMessage(message);

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
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        if (value == null) {
            return fallback;
        }

        String[] methodNames = new String[] {"getInput", "getValue", "get", "doubleValue", "floatValue"};
        for (String methodName : methodNames) {
            try {
                Method method = value.getClass().getMethod(methodName);
                method.setAccessible(true);
                Object result = method.invoke(value);
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            } catch (Throwable ignored) {
            }
        }

        return fallback;
    }
}