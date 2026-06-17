package com.konayuki.statflex.anticheat;

import com.konayuki.statflex.anticheat.event.PacketDetector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;

public final class RuntimePacketHook {
    private static final RuntimePacketHook INSTANCE = new RuntimePacketHook();
    private static final String HANDLER_NAME = "statflex_packet_hook";

    private static boolean registered;

    private NetworkManager hookedManager;
    private Channel hookedChannel;

    private RuntimePacketHook() {
    }

    public static void register() {
        synchronized (RuntimePacketHook.class) {
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

                ChannelInboundHandlerAdapter handler = new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                        if (message instanceof Packet) {
                            MinecraftForge.EVENT_BUS.post(new PacketDetector((Packet<?>) message));
                        }
                        super.channelRead(context, message);
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
