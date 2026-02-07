package com.extrahelden.duelmod.serializer;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

import java.util.UUID;

/**
 * Serializer für GameProfile, um es über SynchedEntityData zu übertragen.
 */
public class GameProfileSerializer {

    public static final EntityDataSerializer<GameProfile> GAME_PROFILE_SERIALIZER = new EntityDataSerializer<>() {

        @Override
        public void write(FriendlyByteBuf buf, GameProfile profile) {
            buf.writeBoolean(profile != null);
            if (profile != null) {
                buf.writeUUID(profile.getId());
                buf.writeUtf(profile.getName() != null ? profile.getName() : "");
            }
        }

        @Override
        public GameProfile read(FriendlyByteBuf buf) {
            if (!buf.readBoolean()) {
                return null;
            }
            UUID id = buf.readUUID();
            String name = buf.readUtf(16);
            return new GameProfile(id, name);
        }

        @Override
        public GameProfile copy(GameProfile profile) {
            return (profile == null) ? null : new GameProfile(profile.getId(), profile.getName());
        }
    };

    /** Muss in der Mod-Initialisierung aufgerufen werden, um den Serializer zu registrieren. */
    public static void register() {
        EntityDataSerializers.registerSerializer(GAME_PROFILE_SERIALIZER);
    }
}
