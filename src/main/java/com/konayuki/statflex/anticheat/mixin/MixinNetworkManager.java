package com.konayuki.statflex.anticheat.mixin;

import com.konayuki.statflex.anticheat.event.ReceivePacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
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
    private void statflex$receivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callbackInfo) {
        MinecraftForge.EVENT_BUS.post(new ReceivePacketEvent(packet));
    }
}
