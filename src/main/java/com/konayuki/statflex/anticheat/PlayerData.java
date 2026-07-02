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
    static final int PACKET_HISTORY_SIZE = 100;
    static final int MIN_HISTORY_SIZE = 40;
    static final int COMBAT_END_DELAY_MS = 10000;
    static final int LAG_RANGE_VL_ALERT = 2;
    
    static final double BURST_RATIO = 4.0D;
    static final int MIN_BURST_PACKETS = 3;
    
    static final double MIN_ZERO_RATE_COMBAT = 0.30D;
    static final double MIN_ZERO_DIFF = 0.20D;
    static final double ZERO_RATIO_THRESHOLD = 2.5D;
    static final double SUSPICIOUS_THRESHOLD = 0.5D;

    int combatPacketHistoryIndex;
    int combatPacketHistoryFilled;
    int normalPacketHistoryIndex;
    int normalPacketHistoryFilled;

    int movementPacketsThisTick;
    int lagRangeVl;
    int consecutiveZeroTicks;

    double lastCombatPacketAverage;
    double lastNormalPacketAverage;
    double lastCombatZeroRate;
    double lastNormalZeroRate;

    long combatUntil;
    long lastSwingEndAt;
    long lastVlIncreaseAt;

    boolean inCombat;
    boolean isSuspicious;
    boolean wasSwinging;

    final int[] combatMovementPacketHistory = new int[PACKET_HISTORY_SIZE];
    final int[] normalMovementPacketHistory = new int[PACKET_HISTORY_SIZE];
    final boolean[] combatZeroPacketHistory = new boolean[PACKET_HISTORY_SIZE];
    final boolean[] normalZeroPacketHistory = new boolean[PACKET_HISTORY_SIZE];
}
