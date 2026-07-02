package com.konayuki.statflex.anticheat;

import com.konayuki.statflex.anticheat.event.ReceivedPacketDetector;
import com.konayuki.statflex.client.ChatManager;
import com.konayuki.statflex.config.Settings;
import com.konayuki.statflex.system.Messages;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
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

public final class Anticheat {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Anticheat INSTANCE = new Anticheat();

    private static final String AUTO_BLOCK = "Auto Block";
    private static final String NO_FALL = "NoFall";
    private static final String NO_SLOW = "NoSlow";
    private static final String SCAFFOLD = "Scaffold";
    private static final String LEGIT_SCAFFOLD = "Legit Scaffold";
    private static final String LAG_RANGE = "Lag Range";

    private static final double MIN_MOVEMENT_SPEED = 0.03D;
    private static final double SERVER_POS_EPSILON = 0.0015D;
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

            if (player.isSwingInProgress && hasNearbyEnemy(player)) {
                data.combatUntil = System.currentTimeMillis() + 5000L;
            }

            performCheck(player, data);
            data.updateServerPos(player);
            data.updateSneak(player);
            players.put(player.getUniqueID(), data);
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

        if (!isLagRangeEligible(player)) {
            System.out.println("[S] Not eligible: " + player.getName());
            return;
        }

        double serverPosX = AnticheatUtils.getServerPosX(player);
        double serverPosZ = AnticheatUtils.getServerPosZ(player);

        if (Double.isNaN(serverPosX)
                || Double.isNaN(serverPosZ)
                || Double.isNaN(data.serverPosX)
                || Double.isNaN(data.serverPosZ)) {
            System.out.println("[S] ServerPos is null: " + player.getName());
            return;
        }

        boolean moving = data.speed > MIN_MOVEMENT_SPEED;

        boolean serverMoved =
                Math.abs(serverPosX - data.serverPosX) > SERVER_POS_EPSILON
                        || Math.abs(serverPosZ - data.serverPosZ) > SERVER_POS_EPSILON;

        boolean freeze = moving && !serverMoved;
        boolean combat = data.isInCombat() && hasNearbyEnemy(player);

        System.out.println("[S] LagRange detection for: " + player.getName()
                + " moving: " + moving
                + " serverMoved: " + serverMoved
                + " freeze: " + freeze
                + " combat: " + combat);

        if (combat) {
            data.pushCombatFreeze(freeze);
        } else {
            data.pushNormalFreeze(freeze);
        }

        System.out.println("[S] Combat history: " + data.combatHistoryFilled
                + " Normal history: " + data.normalHistoryFilled);

        if (data.combatHistoryFilled >= data.MIN_FREEZE_HISTORY_SIZE
                && data.normalHistoryFilled >= data.MIN_FREEZE_HISTORY_SIZE) {

            double combatRate = data.getCombatFreezeRate();
            double normalRate = data.getNormalFreezeRate();

            double diff = combatRate - normalRate;
            double ratio = combatRate / Math.max(normalRate, 0.01D);

            System.out.println("[S] Combat rate: " + combatRate
                    + " normalRate: " + normalRate
                    + " diff: " + diff
                    + " ratio: " + ratio);

            boolean suspicious =
                    diff >= 0.10D &&
                            ratio >= 3.0D;

            System.out.println("[S] Detected suspicious lag: " + suspicious
                    + " patternVl: " + data.lagRangePatternVl);

            if (suspicious) {
                data.lagRangePatternVl++;
            } else {
                data.lagRangePatternVl = Math.max(0, data.lagRangePatternVl - 1);
            }

            if (data.lagRangePatternVl >= 4) {
                System.out.println("[S] " + player.getName() + "flagged LagRange");
                alert(player, LAG_RANGE);
            }
        }

        data.serverPosX = serverPosX;
        data.serverPosZ = serverPosZ;
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

            NetworkPlayerInfo info =
                    mc.getNetHandler().getPlayerInfo(other.getUniqueID());

            if (info == null)
                continue;

            GameProfile profile = info.getGameProfile();

            if (profile == null)
                continue;

            if (profile.getId() != null && profile.getId().version() == 2)
                continue;

            String name = profile.getName();

            IChatComponent display = info.getDisplayName();

            String clean = cleanName(
                    display != null
                            ? display.getFormattedText()
                            : name)
                    .trim()
                    .replaceAll("\\s+", "");

            if (clean.contains("[NPC]")
                    || name.contains("[NPC]"))
                continue;

            if (info.getResponseTime() <= 0)
                continue;

            Team otherTeam = other.getTeam();

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

    private static String cleanName(String name) {
        return name.replaceAll("§[0-9a-frk-o]", "").trim();
    }

    private void debug(String fmt, Object... args) {
        try {
            System.out.printf(fmt + "%n", args);
        } catch (Throwable ignored) {}
    }
}
