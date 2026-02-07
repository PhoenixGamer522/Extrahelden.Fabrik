package com.extrahelden.duelmod.network;

import com.extrahelden.duelmod.DuelMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.extrahelden.duelmod.network.CombatDeathS2CPacket;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";

   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
           new ResourceLocation(DuelMod.MOD_ID, "main"),
           () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
   );





   /** Einmal in commonSetup -> enqueueWork aufrufen. */
  public static void register() {
      int id = 0;

       CHANNEL.registerMessage(
               id++,
               SyncLivesS2CPacket.class,
               SyncLivesS2CPacket::encode,
               SyncLivesS2CPacket::decode,
               SyncLivesS2CPacket::handle
       );

       CHANNEL.registerMessage(
               id++,
               CombatDeathS2CPacket.class,
               CombatDeathS2CPacket::encode,
               CombatDeathS2CPacket::decode,
               CombatDeathS2CPacket::handle
       );

      CHANNEL.registerMessage(
              id++,
              DuelStatusS2CPacket.class,
              DuelStatusS2CPacket::encode,
              DuelStatusS2CPacket::decode,
              DuelStatusS2CPacket::handle
      );
    }



    /** Öffentliche Helper-API fürs Senden. */
     public static void syncLives(ServerPlayer player, int lives, String ownerName, String ownerUuid) {
         boolean linkedActive = (lives == 0) && player.getPersistentData().getBoolean("LinkedHeartActive");
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                 new SyncLivesS2CPacket(lives, ownerName, ownerUuid, linkedActive)
         );
    };

    public static void sendCombatDeath(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CombatDeathS2CPacket()
        );
    }

    public static void sendDuelStatus(ServerPlayer player, boolean inDuel) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new DuelStatusS2CPacket(inDuel)
        );
    }
}

