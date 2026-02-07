package com.extrahelden.duelmod.network;

import com.extrahelden.duelmod.client.ClientForgeEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Notification that the player died while in combat. */
public record CombatDeathS2CPacket() {

    public static void encode(CombatDeathS2CPacket pkt, FriendlyByteBuf buf) {
        // no payload
    }

    public static CombatDeathS2CPacket decode(FriendlyByteBuf buf) {
        return new CombatDeathS2CPacket();
    }

    public static void handle(CombatDeathS2CPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(ClientForgeEvents::markCombatDeath);
        ctx.setPacketHandled(true);
    }
}
