package com.konayuki.statflex.events;

public final class SendChatEvent extends Event implements EventBus.Cancellable {
    private final String message;

    public SendChatEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
