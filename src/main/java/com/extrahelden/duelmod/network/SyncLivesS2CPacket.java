package com.extrahelden.duelmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Lives + LinkedHeart-Owner sync (server -> client) */
public record SyncLivesS2CPacket(int lives, String ownerName, String ownerUuid, boolean linkedActive) {

    public static void encode(SyncLivesS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.lives());
        buf.writeUtf(pkt.ownerName() == null ? "" : pkt.ownerName());
        buf.writeUtf(pkt.ownerUuid() == null ? "" : pkt.ownerUuid());
        buf.writeBoolean(pkt.linkedActive());
    }

    public static SyncLivesS2CPacket decode(FriendlyByteBuf buf) {
        int lives = buf.readInt();
        String on  = buf.readUtf();
        String ou  = buf.readUtf();
        boolean la = buf.readBoolean();
        return new SyncLivesS2CPacket(lives, on, ou, la);
    }

    public static void handle(SyncLivesS2CPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.player != null) {
                var tag = mc.player.getPersistentData();
                tag.putInt("MyLives", pkt.lives());
                tag.putString("LinkedHeartOwner", pkt.ownerName() == null ? "" : pkt.ownerName());
                tag.putString("LinkedHeartOwnerUUID", pkt.ownerUuid() == null ? "" : pkt.ownerUuid());
                tag.putBoolean("LinkedHeartActive", pkt.linkedActive());
            }
        });
        ctx.setPacketHandled(true);
    }
}
