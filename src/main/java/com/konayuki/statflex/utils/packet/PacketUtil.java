package com.konayuki.statflex.utils.packet;

import com.konayuki.statflex.events.ChatEvent;
import com.konayuki.statflex.events.EventBus;
import com.konayuki.statflex.events.PacketEvent;
import com.konayuki.statflex.events.WorldEvent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S07PacketRespawn;

import java.lang.reflect.Field;

public final class PacketUtil {
    private static final PacketUtil INSTANCE = new PacketUtil();
    private static final String HANDLER_NAME = "statflex_packet_hook";

    private static boolean registered;

    private NetworkManager hookedManager;
    private Channel hookedChannel;

    private PacketUtil() {
    }

    public static void register() {
        synchronized (PacketUtil.class) {
            registered = true;
        }
        ensure();
    }

    public static void ensure() {
        if (!registered) {
            return;
        }

        INSTANCE.install();
    }

    private void install() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return;
            }

            install(mc.getNetHandler().getNetworkManager());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private synchronized void install(NetworkManager manager) {
        if (manager == null) {
            return;
        }

        Channel channel = channel(manager);
        if (channel == null || !channel.isOpen()) {
            return;
        }

        if (manager == hookedManager && channel == hookedChannel && channel.pipeline().get(HANDLER_NAME) != null) {
            return;
        }

        Runnable installer = new Runnable() {
            @Override
            public void run() {
                if (channel.pipeline().get(HANDLER_NAME) != null) {
                    return;
                }

                ChannelDuplexHandler handler = new ChannelDuplexHandler() {

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (onReceive(msg)) {
                            super.channelRead(ctx, msg);
                        }
                    }

                    @Override
                    public void write(ChannelHandlerContext ctx,
                                      Object msg,
                                      ChannelPromise promise) throws Exception {

                        if (onSend(msg, promise)) {
                            super.write(ctx, msg, promise);
                        }
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        PacketUtil.this.clearHooked();
                        EventBus.post(new WorldEvent(false, true));
                        super.channelInactive(ctx);
                    }
                };

                if (channel.pipeline().get("packet_handler") != null) {
                    channel.pipeline().addBefore("packet_handler", HANDLER_NAME, handler);
                } else {
                    channel.pipeline().addLast(HANDLER_NAME, handler);
                }
            }
        };

        if (channel.eventLoop().inEventLoop()) {
            installer.run();
        } else {
            channel.eventLoop().execute(installer);
        }

        hookedManager = manager;
        hookedChannel = channel;
    }

    private boolean onReceive(Object msg) {
        if (!(msg instanceof Packet)) {
            return true;
        }

        PacketEvent packetEvent = new PacketEvent((Packet<?>) msg, PacketEvent.Direction.RECEIVE);
        EventBus.post(packetEvent);
        if (packetEvent.isCancelled()) {
            return false;
        }

        if (msg instanceof S01PacketJoinGame || msg instanceof S07PacketRespawn) {
            EventBus.post(new WorldEvent(true, true));
        }

        if (msg instanceof S02PacketChat) {
            S02PacketChat chat = (S02PacketChat) msg;
            ChatEvent chatEvent = new ChatEvent(chat.getChatComponent(), chat.getType());
            EventBus.post(chatEvent);
            if (chatEvent.isCancelled()) {
                return false;
            }
        }

        return true;
    }

    private boolean onSend(Object msg, ChannelPromise promise) {
        if (!(msg instanceof Packet)) {
            return true;
        }

        PacketEvent event = new PacketEvent((Packet<?>) msg, PacketEvent.Direction.SEND);
        EventBus.post(event);
        if (event.isCancelled()) {
            try {
                promise.setSuccess();
            } catch (Throwable ignored) {
            }
            return false;
        }

        return true;
    }

    private synchronized void clearHooked() {
        hookedManager = null;
        hookedChannel = null;
    }

    private Channel channel(NetworkManager manager) {
        Class<?> type = manager.getClass();
        while (type != null) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (!Channel.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    return (Channel) field.get(manager);
                } catch (IllegalAccessException ignored) {
                }
            }

            type = type.getSuperclass();
        }

        return null;
    }
}
