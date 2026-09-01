package com.konayuki.statflex.events;

public final class WorldEvent extends Event {
    private final boolean joined;
    private final boolean localPlayer;

    public WorldEvent(boolean joined, boolean localPlayer) {
        this.joined = joined;
        this.localPlayer = localPlayer;
    }

    public boolean isJoined() {
        return joined;
    }

    public boolean isLocalPlayer() {
        return localPlayer;
    }
}
