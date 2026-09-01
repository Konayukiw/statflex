package com.konayuki.statflex.utils.mixin;

import com.konayuki.statflex.events.EventBus;
import com.konayuki.statflex.events.PacketEvent;

import io.netty.channel.ChannelHandlerContext;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
            at = @At("HEAD")
    )
    private void statflex$receive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callbackInfo) {
        EventBus.post(new PacketEvent(packet, PacketEvent.Direction.RECEIVE));
    }
}
