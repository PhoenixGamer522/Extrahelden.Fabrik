///package com.extrahelden.duelmod.network;

///import net.minecraft.server.level.ServerPlayer;
///import net.minecraftforge.network.PacketDistributor;

///public final class HeartSync {
    ///    private HeartSync() {}

    ///   public static void sync(ServerPlayer player) {
    ///       var tag = player.getPersistentData();
    ///       int lives = tag.getInt("MyLives");
    ///      String tex = tag.getString("LinkedHeartTexture"); // "" wenn nicht gesetzt
    ///
    ///     NetworkHandler.CHANNEL.send(
    ///            PacketDistributor.PLAYER.with(() -> player),
    ///           new SyncLivesS2CPacket(lives, o)
    ///   );
    /// }
    ///}