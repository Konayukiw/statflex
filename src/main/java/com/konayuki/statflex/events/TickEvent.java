package com.konayuki.statflex.events;

public final class TickEvent extends Event {
    private static final TickEvent INSTANCE = new TickEvent();

    public static TickEvent get() {
        return INSTANCE;
    }
}
