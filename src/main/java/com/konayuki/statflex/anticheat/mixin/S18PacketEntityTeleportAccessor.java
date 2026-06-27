package com.konayuki.statflex.anticheat.mixin;

import net.minecraft.network.play.server.S18PacketEntityTeleport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(S18PacketEntityTeleport.class)
public interface S18PacketEntityTeleportAccessor {
    @Accessor("entityId")
    int getEntityId();
}