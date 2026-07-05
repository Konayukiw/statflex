package com.konayuki.statflex.anticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;

final class PlayerData {
    int fastTick;
    int autoBlockTicks;
    int ticksExisted;
    int lastSneakTick;
    int sneakTicks;
    int noSlowTicks;
    int aboveVoidTicks;

    int speedHistoryIndex;
    int speedHistoryFilled;

    double speed;
    double posX, posY, posZ;
    double serverPosX = Double.NaN;
    double serverPosY = Double.NaN;
    double serverPosZ = Double.NaN;

    boolean sneaking;
    EntityPlayer player;

    final double[] speedHistory = new double[SPEED_HISTORY_SIZE];

    static final int SPEED_HISTORY_SIZE = 6;

    static final int MIN_BURST_PACKETS = 3;
    static final int FREEZE_TICKS_THRESHOLD = 2;
    static final int BURST_WINDOW_TICKS = 4;
    static final int LAG_RANGE_VL_ALERT = 3;
    static final long VL_DECAY_STEP_MS = 15000L / LAG_RANGE_VL_ALERT;

    int movementPacketsThisTick;
    int lagRangeVl;
    int consecutiveZeroTicks;

    boolean approaching;
    double lastDistanceSq = Double.NaN;

    boolean waitingForBurst;
    int waitingTicks;

    boolean wasSwinging;

    long lastVlIncreaseAt;

    void update(EntityPlayer player) {
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
            if (AnticheatUtils.getSprintingTicksLeft(player) == 1) {
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

    void updateSneak(EntityPlayer player) {
        sneaking = player.isSneaking();
    }

    void updateServerPos(EntityPlayer player) {
        serverPosX = AnticheatUtils.getServerPosX(player);
        serverPosY = AnticheatUtils.getServerPosY(player);
        serverPosZ = AnticheatUtils.getServerPosZ(player);
    }

    private void pushSpeedHistory(double value) {
        speedHistory[speedHistoryIndex] = value;
        speedHistoryIndex = (speedHistoryIndex + 1) % SPEED_HISTORY_SIZE;
        if (speedHistoryFilled < SPEED_HISTORY_SIZE) {
            speedHistoryFilled++;
        }
    }
}