package com.konayuki.statflex.anticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

final class LagModulesDetector {
    static final String CHEAT_NAME = "Blink/Lag Range";

    private static final int PACKET_GAP_TICKS = 8;
    private static final int ALERT_SCORE = 5;
    private static final double DELTA_V_THRESHOLD = 0.35D;
    private static final double MIN_MOVEMENT_SPEED = 0.03D;
    private static final double LENIENCY_BUFFER = 0.18D;
    private static final double SERVER_POS_EPSILON = 0.001D;

    private LagModulesDetector() {
    }

    static void check(EntityPlayer player, PlayerData data, AlertCallback alertCallback) {
        if (!isEligible(player)) {
            return;
        }

        long now = System.currentTimeMillis();
        double serverPosX = AnticheatUtils.getServerPosX(player);
        double serverPosY = AnticheatUtils.getServerPosY(player);
        double serverPosZ = AnticheatUtils.getServerPosZ(player);

        if (Double.isNaN(serverPosX) || Double.isNaN(serverPosY) || Double.isNaN(serverPosZ)
                || Double.isNaN(data.serverPosX) || Double.isNaN(data.serverPosY) || Double.isNaN(data.serverPosZ)) {
            return;
        }

        boolean serverPosChanged = Math.abs(data.serverPosX - serverPosX) > SERVER_POS_EPSILON
                || Math.abs(data.serverPosY - serverPosY) > SERVER_POS_EPSILON
                || Math.abs(data.serverPosZ - serverPosZ) > SERVER_POS_EPSILON;

        if (!serverPosChanged) {
            data.ticksWithoutServerPosUpdate++;

            if (data.ticksWithoutServerPosUpdate >= PACKET_GAP_TICKS && !data.packetGapScored) {
                data.blinkFlagScore.addPoint(now);
                data.packetGapScored = true;
            }
        } else {
            if (data.ticksWithoutServerPosUpdate >= PACKET_GAP_TICKS && !data.postGapBpsScored) {
                double deltaX = serverPosX - data.serverPosX;
                double deltaZ = serverPosZ - data.serverPosZ;
                double horizontalDistance = Math.hypot(deltaX, deltaZ);

                if (horizontalDistance > getMaxBlocksPerTick(player)) {
                    data.blinkFlagScore.addPoint(now);
                    data.postGapBpsScored = true;
                }
            }

            data.ticksWithoutServerPosUpdate = 0;
            data.packetGapScored = false;
            data.postGapBpsScored = false;
        }

        boolean justResumedAfterGap = serverPosChanged
                && data.ticksWithoutServerPosUpdate >= PACKET_GAP_TICKS;

        double deltaV = Math.abs(data.speed - data.lastHorizontalSpeed);
        if (justResumedAfterGap && deltaV > DELTA_V_THRESHOLD
                && data.speed >= MIN_MOVEMENT_SPEED
                && data.lastHorizontalSpeed >= MIN_MOVEMENT_SPEED) {
            data.blinkFlagScore.addPoint(now);
        }

        data.lastHorizontalSpeed = data.speed;

        if (data.blinkFlagScore.getScore(now) >= ALERT_SCORE) {
            alertCallback.alert(player, CHEAT_NAME);
            data.blinkFlagScore.clear();
            data.ticksWithoutServerPosUpdate = 0;
            data.packetGapScored = false;
            data.postGapBpsScored = false;
        }
    }

    private static boolean isEligible(EntityPlayer player) {
        return player.ticksExisted >= 60
                && !player.isInWater()
                && !player.isInLava()
                && !AnticheatUtils.onLadder(player);
    }

    private static double getMaxBlocksPerTick(EntityPlayer player) {
        double base = 0.221D;

        if (player.isPotionActive(Potion.moveSpeed)) {
            PotionEffect effect = player.getActivePotionEffect(Potion.moveSpeed);
            if (effect != null) {
                base *= 1.0D + 0.2D * (effect.getAmplifier() + 1);
            }
        }

        if (player.isSprinting()) {
            base *= 1.3D;
        }

        return base + LENIENCY_BUFFER;
    }

    interface AlertCallback {
        void alert(EntityPlayer player, String cheat);
    }
}
