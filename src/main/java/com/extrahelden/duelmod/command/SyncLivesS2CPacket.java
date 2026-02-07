package com.extrahelden.duelmod.command;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sync: Spieler-Leben + Linked-Heart-Textur (pro Spieler) + sofortige HUD/Actionbar-Aktualisierung. */
public record SyncLivesS2CPacket(int lives, String linkedHeartTex) {

    /** Encode */
 public static void encode(SyncLivesS2CPacket pkt, FriendlyByteBuf buf) {
       buf.writeInt(pkt.lives());
      buf.writeUtf(pkt.linkedHeartTex() == null ? "" : pkt.linkedHeartTex());
  }

      /** Decode */
       public static SyncLivesS2CPacket decode(FriendlyByteBuf buf) {
          int lives = buf.readInt();
         String tex = buf.readUtf();
          return new SyncLivesS2CPacket(lives, tex);
      }

      /** Handle (Client) */
      /* Handle (Client) */
      public static void handle(SyncLivesS2CPacket pkt, java.util.function.Supplier<NetworkEvent.Context> ctxSupplier) {
          NetworkEvent.Context ctx = ctxSupplier.get();
          ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.handle(pkt)));
          ctx.setPacketHandled(true);
      }

    /** Reiner Client-Teil, damit der Server diese Klasse nicht laden muss. */
    private static final class Client {
        private static void handle(SyncLivesS2CPacket pkt) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            // 1) Clientseitiges NBT aktualisieren
            var tag = mc.player.getPersistentData();
            tag.putInt("MyLives", pkt.lives());
            tag.putString("LinkedHeartTexture", pkt.linkedHeartTex() == null ? "" : pkt.linkedHeartTex());

            // 2) Actionbar sofort updaten
            if (pkt.lives() > 0) {
                mc.player.displayClientMessage(
                        Component.literal("§bLeben: §f" + pkt.lives()),
                        true // Actionbar
                );
            } else {
                // lives == 0 -> Linked Heart sichtbar/aktiv
                mc.player.displayClientMessage(
                        Component.literal("§dLinked Heart §7ist aktiv"),
                        true
                );
            }
        }
    }
}

