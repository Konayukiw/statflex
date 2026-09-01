package com.konayuki.statflex.events;

import net.minecraft.util.IChatComponent;

public final class ChatEvent extends Event implements EventBus.Cancellable {
    private final IChatComponent message;
    private final int type;

    public ChatEvent(IChatComponent message, int type) {
        this.message = message;
        this.type = type;
    }

    public IChatComponent getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }
}
