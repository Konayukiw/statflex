package com.konayuki.statflex.anticheat;

import net.minecraft.entity.player.EntityPlayer;

final class PlayerData {
    int burstHistoryIndex = 0;
    int burstHistoryFilled = 0;

    int fastTick;
    int autoBlockTicks;
    int ticksExisted;
    int lastSneakTick;
    int sneakTicks;
    int noSlowTicks;
    int aboveVoidTicks;
    int consecutiveFrozenTicks;
    int movePacketsSinceFreeze;
    int freezeCandidateTicks;
    int lagRangePatternVl;
    int speedHistoryIndex = 0;
    int speedHistoryFilled = 0;

    double speed;
    double posX, posY, posZ;
    double serverPosX = Double.NaN;
    double serverPosY = Double.NaN;
    double serverPosZ = Double.NaN;
    double freezeStartServerPosX = Double.NaN;
    double freezeStartServerPosZ = Double.NaN;
    double preFreezeAverageSpeed = Double.NaN;

    long lastLagRangeBurstTime;
    long lagRangeStateEnteredAt;
    boolean burstHadRealMove;

    static final int SPEED_HISTORY_SIZE = 6;
    static final int BURST_HISTORY_SIZE = 5;
    final int[] burstFrozenTicksHistory = new int[BURST_HISTORY_SIZE];
    final double[] speedHistory = new double[SPEED_HISTORY_SIZE];

    boolean sneaking;

    EntityPlayer player;

    enum LagRangeState {
        IDLE,
        WAITING_FREEZE,
        FROZEN
    }

    LagRangeState lagRangeState = LagRangeState.IDLE;

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
                && player.getHeldItem().getItem() instanceof net.minecraft.item.ItemBlock) {
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

    void pushSpeedHistory(double value) {
        speedHistory[speedHistoryIndex] = value;
        speedHistoryIndex = (speedHistoryIndex + 1) % SPEED_HISTORY_SIZE;
        if (speedHistoryFilled < SPEED_HISTORY_SIZE) speedHistoryFilled++;
    }

    double getAverageSpeed() {
        if (speedHistoryFilled == 0) return 0.0;
        double total = 0.0;
        for (int i = 0; i < speedHistoryFilled; i++) total += speedHistory[i];
        return total / speedHistoryFilled;
    }

    double computePreFreezeSpeedVariance() {
        if (speedHistoryFilled < SPEED_HISTORY_SIZE) return Double.MAX_VALUE;
        double mean = 0.0D;
        for (double v : speedHistory) mean += v;
        mean /= speedHistoryFilled;
        double variance = 0.0D;
        for (double v : speedHistory) {
            double diff = v - mean;
            variance += diff * diff;
        }
        variance /= speedHistoryFilled;
        return variance;
    }

    void pushBurstFrozenTicks(int frozenTicks) {
        burstFrozenTicksHistory[burstHistoryIndex] = frozenTicks;
        burstHistoryIndex = (burstHistoryIndex + 1) % BURST_HISTORY_SIZE;
        if (burstHistoryFilled < BURST_HISTORY_SIZE) burstHistoryFilled++;
    }

    int computeBurstFrozenTicksSpread() {
        if (burstHistoryFilled < BURST_HISTORY_SIZE) return Integer.MAX_VALUE;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < burstHistoryFilled; i++) {
            int v = burstFrozenTicksHistory[i];
            if (v < min) min = v;
            if (v > max) max = v;
        }
        return max - min;
    }

    void resetBurstHistory() {
        burstHistoryIndex = 0;
        burstHistoryFilled = 0;
    }
}
