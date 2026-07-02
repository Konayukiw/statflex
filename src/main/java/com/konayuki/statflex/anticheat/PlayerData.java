package com.konayuki.statflex.anticheat;

import net.minecraft.entity.player.EntityPlayer;

final class PlayerData {
    int fastTick;
    int autoBlockTicks;
    int ticksExisted;
    int lastSneakTick;
    int sneakTicks;
    int noSlowTicks;
    int aboveVoidTicks;
    int lagRangePatternVl;
    int speedHistoryIndex;
    int speedHistoryFilled;
    int combatHistoryIndex;
    int normalHistoryIndex;
    int combatHistoryFilled;
    int normalHistoryFilled;

    double speed;
    double posX, posY, posZ;
    double serverPosX = Double.NaN;
    double serverPosY = Double.NaN;
    double serverPosZ = Double.NaN;

    long combatUntil;

    static final int SPEED_HISTORY_SIZE = 6;
    static final int FREEZE_HISTORY_SIZE = 64;
    static final int MIN_FREEZE_HISTORY_SIZE = 24;


    final double[] speedHistory = new double[SPEED_HISTORY_SIZE];
    final boolean[] combatFreezeHistory = new boolean[FREEZE_HISTORY_SIZE];
    final boolean[] normalFreezeHistory = new boolean[FREEZE_HISTORY_SIZE];

    boolean sneaking;
    boolean lagRangeAboveThreshold;
    boolean isInCombat() {
        return System.currentTimeMillis() < combatUntil;
    }

    EntityPlayer player;

    double getCombatFreezeRate() {
        if (combatHistoryFilled == 0)
            return 0;

        int freeze = 0;

        for (int i = 0; i < combatHistoryFilled; i++)
            if (combatFreezeHistory[i])
                freeze++;

        return (double) freeze / combatHistoryFilled;
    }

    double getNormalFreezeRate() {
        if (normalHistoryFilled == 0)
            return 0;

        int freeze = 0;

        for (int i = 0; i < normalHistoryFilled; i++)
            if (normalFreezeHistory[i])
                freeze++;

        return (double) freeze / normalHistoryFilled;
    }

    void pushCombatFreeze(boolean freeze) {
        combatFreezeHistory[combatHistoryIndex] = freeze;
        combatHistoryIndex = (combatHistoryIndex + 1) % FREEZE_HISTORY_SIZE;

        if (combatHistoryFilled < FREEZE_HISTORY_SIZE)
            combatHistoryFilled++;
    }

    void pushNormalFreeze(boolean freeze) {
        normalFreezeHistory[normalHistoryIndex] = freeze;
        normalHistoryIndex = (normalHistoryIndex + 1) % FREEZE_HISTORY_SIZE;

        if (normalHistoryFilled < FREEZE_HISTORY_SIZE)
            normalHistoryFilled++;
    }

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
}
