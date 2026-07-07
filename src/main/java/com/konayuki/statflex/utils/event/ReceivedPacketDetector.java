package com.konayuki.statflex.utils.event;

import net.minecraft.network.Packet;

import net.minecraftforge.fml.common.eventhandler.Event;

public final class ReceivedPacketDetector extends Event {
    private final Packet<?> packet;

    public ReceivedPacketDetector(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}