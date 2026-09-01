package com.konayuki.statflex.events;

public final class MouseEvent extends Event implements EventBus.Cancellable {
    private final int dwheel;

    public MouseEvent(int dwheel) {
        this.dwheel = dwheel;
    }

    public int getDWheel() {
        return dwheel;
    }
}
