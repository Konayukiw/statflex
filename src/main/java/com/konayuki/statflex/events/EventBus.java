package com.konayuki.statflex.events;

import com.konayuki.statflex.utils.Debug;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventBus {

    private static final Object LOCK = new Object();
    private static final Map<Class<?>, List<Listener>> LISTENERS = new HashMap<Class<?>, List<Listener>>();

    private EventBus() {
    }

    public static void register(Object handler) {
        synchronized (LOCK) {
            for (Class<?> type = handler.getClass(); type != null; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(Subscribe.class)) {
                        continue;
                    }
                    if (method.getParameterTypes().length != 1
                            || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        continue;
                    }
                    if (isStatic(method)) {
                        continue;
                    }
                    method.setAccessible(true);
                    Class<?> eventClass = method.getParameterTypes()[0];
                    List<Listener> listeners = LISTENERS.get(eventClass);
                    if (listeners == null) {
                        listeners = new ArrayList<Listener>();
                        LISTENERS.put(eventClass, listeners);
                    }
                    listeners.add(new Listener(handler, method));
                }
            }
        }
    }

    private static boolean isStatic(Method method) {
        int modifiers = method.getModifiers();
        return java.lang.reflect.Modifier.isStatic(modifiers);
    }

    public static <E extends Event> E post(E event) {
        for (Class<?> type = event.getClass(); type != null && Event.class.isAssignableFrom(type);
             type = type.getSuperclass()) {
            List<Listener> listeners;
            synchronized (LOCK) {
                List<Listener> known = LISTENERS.get(type);
                listeners = known == null ? null : new ArrayList<Listener>(known);
            }
            if (listeners == null) {
                continue;
            }
            for (Listener listener : listeners) {
                if (event.isCancelled() && stopsOnCancel(event)) {
                    break;
                }
                listener.invoke(event);
            }
        }
        return event;
    }

    private static boolean stopsOnCancel(Event event) {
        return event instanceof Cancellable;
    }

    public static void clear() {
        synchronized (LOCK) {
            LISTENERS.clear();
        }
    }

    public static int listenerCount() {
        synchronized (LOCK) {
            int total = 0;
            for (List<Listener> listeners : LISTENERS.values()) {
                total += listeners.size();
            }
            return total;
        }
    }

    private static final class Listener {
        private final Object handler;
        private final Method method;

        Listener(Object handler, Method method) {
            this.handler = handler;
            this.method = method;
        }

        void invoke(Event event) {
            try {
                method.invoke(handler, event);
            } catch (Throwable throwable) {
                Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
                Debug.log("Event listener " + handler.getClass().getSimpleName()
                        + "." + method.getName() + " failed: " + cause);
            }
        }
    }

    private static final List<Class<?>> EMPTY = Collections.emptyList();

    static List<Class<?>> unused() {
        return EMPTY;
    }

    public interface Cancellable {
    }
}
