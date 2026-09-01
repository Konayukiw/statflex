package com.konayuki.statflex.inject;

import com.konayuki.statflex.inject.HookRegistry.Position;

public final class Hook {
    private static final String V = "()V";
    private static final String MINECRAFT = "net.minecraft.client.Minecraft";
    private static final String LOCAL_PLAYER = "net.minecraft.client.entity.EntityPlayerSP";
    private static final String TAB_OVERLAY = "net.minecraft.client.gui.GuiPlayerTabOverlay";
    private static final String TAB_OVERLAY_DESC =
            "(ILnet/minecraft/scoreboard/Scoreboard;"
                    + "Lnet/minecraft/scoreboard/ScoreObjective;)V";

    private Hook() {
    }

    public static void register() {
        HookRegistry.hook(MINECRAFT, "runTick", V)
                .at(Position.RETURN).calls("tickPost").add();

        HookRegistry.hook(TAB_OVERLAY, "renderPlayerlist", TAB_OVERLAY_DESC)
                .at(Position.HEAD).cancellable().calls("renderTab", "()Z").add();

        HookRegistry.hook(MINECRAFT, "runTick", V)
                .at(Position.REPLACE_INVOKE)
                .invokingUnmapped("org.lwjgl.input.Mouse", "getEventDWheel", "()I")
                .calls("mouseWheel", "()I").add();

        HookRegistry.hook(LOCAL_PLAYER, "sendChatMessage", "(Ljava/lang/String;)V")
                .at(Position.HEAD).cancellable().args("0")
                .calls("sendChatMessage", "(Ljava/lang/Object;)Z").add();
    }
}
