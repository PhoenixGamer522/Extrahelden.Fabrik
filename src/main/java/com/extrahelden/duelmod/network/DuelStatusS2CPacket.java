package com.extrahelden.duelmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Notifies the client to hide or show the heart HUD while in a duel.
 */
public record DuelStatusS2CPacket(boolean inDuel) {

    public static void encode(DuelStatusS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.inDuel());
    }

    public static DuelStatusS2CPacket decode(FriendlyByteBuf buf) {
        return new DuelStatusS2CPacket(buf.readBoolean());
    }

    public static void handle(DuelStatusS2CPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getPersistentData().putBoolean("InDuel", pkt.inDuel());
            }
        });
        ctx.setPacketHandled(true);
    }
}