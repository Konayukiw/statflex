package com.konayuki.statflex.inject;

import com.konayuki.statflex.events.EventBus;
import com.konayuki.statflex.events.MouseEvent;
import com.konayuki.statflex.events.RenderTabEvent;
import com.konayuki.statflex.events.SendChatEvent;
import com.konayuki.statflex.events.TickEvent;

public final class Callback {
    public static final String OWNER = "com/konayuki/statflex/inject/Callback";
    private Callback() {
    }

    public static void tickPost() {
        try {
            Bootstrap.tick();
            NativeBridge.flushTransformLog();
            if (!Bootstrap.isStarted()) {
                return;
            }
            EventBus.post(TickEvent.get());
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
        }
    }

    public static boolean renderTab() {
        try {
            if (!Bootstrap.isStarted()) {
                return false;
            }
            RenderTabEvent event = new RenderTabEvent(0.0F);
            EventBus.post(event);
            return event.isCancelled();
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return false;
        }
    }

    public static int mouseWheel() {
        try {
            int wheel = org.lwjgl.input.Mouse.getEventDWheel();
            if (Bootstrap.isStarted() && wheel != 0) {
                MouseEvent event = new MouseEvent(wheel);
                EventBus.post(event);
                if (event.isCancelled()) {
                    return 0;
                }
            }
            return wheel;
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return 0;
        }
    }

    public static boolean sendChatMessage(Object raw) {
        try {
            if (!Bootstrap.isStarted() || !(raw instanceof String)) {
                return false;
            }
            SendChatEvent event = new SendChatEvent((String) raw);
            EventBus.post(event);
            return event.isCancelled();
        } catch (Throwable swallowed) {
            Log.swallowed(swallowed);
            return false;
        }
    }
}
