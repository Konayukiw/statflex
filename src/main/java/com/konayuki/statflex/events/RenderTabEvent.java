package com.konayuki.statflex.events;

public final class RenderTabEvent extends Event {
    private final float partialTicks;

    public RenderTabEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
