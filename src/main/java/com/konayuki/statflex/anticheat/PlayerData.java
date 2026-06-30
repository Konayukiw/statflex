package com.konayuki.statflex.anticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;

final class PlayerData {
    double speed;
    int aboveVoidTicks;
    int fastTick;
    int autoBlockTicks;
    int ticksExisted;
    int lastSneakTick;
    int ticksWithoutServerPosUpdate;
    int sneakTicks;
    int noSlowTicks;
    int consecutiveFrozenTicks = 0;
    int movePacketsSinceFreeze;
    int lagRangePatternVl;

    double posZ;
    double posY;
    double posX;
    double serverPosX = Double.NaN;
    double serverPosY = Double.NaN;
    double serverPosZ = Double.NaN;
    double lastHorizontalSpeed;
    boolean sneaking;
    boolean burstHadRealMove = false;
    long lagRangeStateEnteredAt = 0L;
    long lastLagRangeBurstTime = 0L;
    long lastAttackPacketTime = Long.MIN_VALUE;

    enum LagRangeState { IDLE, WAITING_FREEZE, FROZEN }
    LagRangeState lagRangeState = LagRangeState.IDLE;

    void update(EntityPlayer player) {
        int currentTicks = player.ticksExisted;
        posX = player.posX - player.prevPosX;
        posY = player.posY - player.prevPosY;
        posZ = player.posZ - player.prevPosZ;
        speed = Math.max(Math.abs(posX), Math.abs(posZ));

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
}
