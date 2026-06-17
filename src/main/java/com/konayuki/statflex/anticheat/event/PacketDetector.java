package com.konayuki.statflex.anticheat.event;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Event;

public final class PacketDetector extends Event {
    private final Packet<?> packet;

    public PacketDetector(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}