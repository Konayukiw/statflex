package com.konayuki.statflex.utils;

import com.konayuki.statflex.utils.event.ReceivedPacketDetector;
import com.konayuki.statflex.utils.event.SentPacketDetector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandlerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;

public final class PacketUtils {
    private static final PacketUtils INSTANCE = new PacketUtils();
    private static final String HANDLER_NAME = "statflex_packet_hook";

    private static boolean registered;

    private NetworkManager hookedManager;
    private Channel hookedChannel;

    private PacketUtils() {
    }

    public static void register() {
        synchronized (PacketUtils.class) {
            if (!registered) {
                MinecraftForge.EVENT_BUS.register(INSTANCE);
                registered = true;
            }
        }

        ensureInstalled();
    }

    public static void ensureInstalled() {
        if (!registered) {
            return;
        }

        INSTANCE.installCurrent();
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        installCurrent();
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        synchronized (this) {
            hookedManager = null;
            hookedChannel = null;
        }
    }

    private void installCurrent() {
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

        Channel channel = findChannel(manager);
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
                        if (msg instanceof Packet) {
                            MinecraftForge.EVENT_BUS.post(new ReceivedPacketDetector((Packet<?>) msg));
                        }
                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void write(ChannelHandlerContext ctx,
                                      Object msg,
                                      ChannelPromise promise) throws Exception {

                        if (msg instanceof Packet) {
                            MinecraftForge.EVENT_BUS.post(new SentPacketDetector((Packet<?>) msg));
                        }

                        super.write(ctx, msg, promise);
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

    private Channel findChannel(NetworkManager manager) {
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
