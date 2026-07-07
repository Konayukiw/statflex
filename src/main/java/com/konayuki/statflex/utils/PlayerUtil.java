package com.konayuki.statflex.utils;

import com.konayuki.statflex.features.anticheat.AnticheatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;

public final class PlayerUtil {
    public int fastTick;
    public int autoBlockTicks;
    public int ticksExisted;
    public int lastSneakTick;
    public int sneakTicks;
    public int noSlowTicks;
    public int aboveVoidTicks;

    public int speedHistoryIndex;
    public int speedHistoryFilled;

    public double speed;
    public double posX, posY, posZ;
    public double serverPosX = Double.NaN;
    public double serverPosY = Double.NaN;
    public double serverPosZ = Double.NaN;

    public boolean sneaking;
    public EntityPlayer player;

    public final double[] speedHistory = new double[SPEED_HISTORY_SIZE];

    public static final int SPEED_HISTORY_SIZE = 6;

    public static final int MIN_BURST_PACKETS = 3;
    public static final int FREEZE_TICKS_THRESHOLD = 2;
    public static final int BURST_WINDOW_TICKS = 4;
    public static final int LAG_RANGE_VL_ALERT = 3;
    public static final long VL_DECAY_STEP_MS = 15000L / LAG_RANGE_VL_ALERT;

    public int movementPacketsThisTick;
    public int lagRangeVl;
    public int consecutiveZeroTicks;

    public boolean approaching;
    public double lastDistanceSq = Double.NaN;

    public boolean waitingForBurst;
    public int waitingTicks;

    public boolean wasSwinging;

    public long lastVlIncreaseAt;

    public void update(EntityPlayer player) {
        int currentTicks = player.ticksExisted;
        posX = player.posX - player.prevPosX;
        posY = player.posY - player.prevPosY;
        posZ = player.posZ - player.prevPosZ;
        speed = Math.max(Math.abs(posX), Math.abs(posZ));

        pushSpeedHistory(speed);

        if (speed >= 0.07D) {
            fastTick++;
            ticksExisted = currentTicks;
        } else {
            fastTick = 0;
        }

        if (Math.abs(posY) >= 0.1D) {
            aboveVoidTicks = currentTicks;
        }

        if (player.isSneaking()) {
            lastSneakTick = currentTicks;
        }

        if (player.isSwingInProgress && player.isBlocking()) {
            autoBlockTicks++;
        } else {
            autoBlockTicks = 0;
        }

        if (player.isSprinting() && player.isUsingItem()) {
            noSlowTicks++;
        } else {
            noSlowTicks = 0;
        }

        if (player.rotationPitch >= 70.0F
                && player.getHeldItem() != null
                && player.getHeldItem().getItem() instanceof ItemBlock) {
            if (AnticheatUtil.getSprintingTicksLeft(player) == 1) {
                if (!sneaking && player.isSneaking()) {
                    sneakTicks++;
                } else {
                    sneakTicks = 0;
                }
            }
        } else {
            sneakTicks = 0;
        }
    }

    public void updateSneak(EntityPlayer player) {
        sneaking = player.isSneaking();
    }

    public void updateServerPos(EntityPlayer player) {
        serverPosX = AnticheatUtil.getServerPosX(player);
        serverPosY = AnticheatUtil.getServerPosY(player);
        serverPosZ = AnticheatUtil.getServerPosZ(player);
    }

    private void pushSpeedHistory(double value) {
        speedHistory[speedHistoryIndex] = value;
        speedHistoryIndex = (speedHistoryIndex + 1) % SPEED_HISTORY_SIZE;
        if (speedHistoryFilled < SPEED_HISTORY_SIZE) {
            speedHistoryFilled++;
        }
    }
}