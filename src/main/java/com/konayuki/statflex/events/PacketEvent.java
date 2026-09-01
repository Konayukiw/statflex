package com.konayuki.statflex.events;

import net.minecraft.network.Packet;

public final class PacketEvent extends Event implements EventBus.Cancellable {
    public enum Direction {
        RECEIVE, SEND
    }

    private final Packet<?> packet;
    private final Direction direction;

    public PacketEvent(Packet<?> packet, Direction direction) {
        this.packet = packet;
        this.direction = direction;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public Direction getDirection() {
        return direction;
    }
}
