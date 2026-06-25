package com.konayuki.statflex.anticheat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

final class LagRangeDetector {
    static final String CHEAT_NAME = "Lag Range";

    private static final int PACKET_GAP_TICKS = 4;
    private static final int ALERT_SCORE = 4;
    private static final double MIN_MOVEMENT_SPEED = 0.03D;
    private static final double LENIENCY_BUFFER = 0.18D;
    private static final double SERVER_POS_EPSILON = 0.001D;
    private static final int POST_ATTACK_MONITOR_TICKS = 5;

    private LagRangeDetector() {
    }

    static void check(EntityPlayer player, PlayerData data, AlertCallback alertCallback) {
        if (!isEligible(player)) {
            return;
        }

        long now = System.currentTimeMillis();
        double serverPosX = AnticheatUtils.getServerPosX(player);
        double serverPosZ = AnticheatUtils.getServerPosZ(player);

        if (Double.isNaN(serverPosX) || Double.isNaN(serverPosZ)
                || Double.isNaN(data.serverPosX) || Double.isNaN(data.serverPosZ)) {
            return;
        }

        boolean isMoving = data.speed > MIN_MOVEMENT_SPEED;
        boolean serverPosChanged = Math.abs(data.serverPosX - serverPosX) > SERVER_POS_EPSILON
                || Math.abs(data.serverPosZ - serverPosZ) > SERVER_POS_EPSILON;

        boolean packetFrozen = isMoving && !serverPosChanged;

        if (packetFrozen) {
            data.ticksWithoutServerPosUpdate++;
        } else if (serverPosChanged) {
            if (data.ticksWithoutServerPosUpdate >= PACKET_GAP_TICKS) {

                double totalDelta = Math.hypot(
                        serverPosX - data.gapStartX,
                        serverPosZ - data.gapStartZ
                );
                double maxAllowed = getMaxBlocksPerTick(player) * data.ticksWithoutServerPosUpdate;

                if (totalDelta > maxAllowed * 0.7D) {
                    data.blinkFlagScore.addPoint(now);
                }

                if (!data.packetGapScored) {
                    data.blinkFlagScore.addPoint(now);
                    data.packetGapScored = true;
                }
            }

            data.ticksWithoutServerPosUpdate = 0;
            data.packetGapScored = false;
            data.gapStartX = serverPosX;
            data.gapStartZ = serverPosZ;
        }

        // ギャップ開始時の座標を記録
        if (packetFrozen && data.ticksWithoutServerPosUpdate == 1) {
            data.gapStartX = data.serverPosX;
            data.gapStartZ = data.serverPosZ;
        }

        // --- 3. 攻撃後バースト検知 ---
        // ソースより: C02PacketUseEntity送信でsetDelay(0)が呼ばれ
        // 溜まった移動パケットが一気に送信される
        if (data.postAttackMonitorTicks > 0) {
            data.postAttackMonitorTicks--;
            if (serverPosChanged) {
                data.packetsInBurst++;
                // 攻撃後5tick以内に3回以上位置更新 = バースト送信
                if (data.packetsInBurst >= 3 && data.ticksWithoutServerPosUpdate == 0) {
                    data.blinkFlagScore.addPoint(now);
                }
            }
        }

        // 攻撃パケット検知時に監視開始（PlayerDataで管理）
        if (data.attackPacketReceived) {
            data.postAttackMonitorTicks = POST_ATTACK_MONITOR_TICKS;
            data.packetsInBurst = 0;
            data.attackPacketReceived = false;
        }

        data.serverPosX = serverPosX;
        data.serverPosZ = serverPosZ;
        data.lastHorizontalSpeed = data.speed;

        if (data.blinkFlagScore.getScore(now) >= ALERT_SCORE) {
            alertCallback.alert(player, CHEAT_NAME);
            data.blinkFlagScore.clear();
            data.ticksWithoutServerPosUpdate = 0;
            data.packetGapScored = false;
            data.gapStartX = 0;
            data.gapStartZ = 0;
        }
    }

    private static boolean isEligible(EntityPlayer player) {
        return player.ticksExisted >= 60
                && !player.isInWater()
                && !player.isInLava()
                && !player.isRiding()
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