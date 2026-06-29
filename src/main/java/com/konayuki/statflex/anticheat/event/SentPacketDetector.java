package com.konayuki.statflex.anticheat.event;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Event;

public final class SentPacketDetector extends Event {

    private final Packet<?> packet;

    public SentPacketDetector(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}