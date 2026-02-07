package com.extrahelden.duelmod.command;

import com.extrahelden.duelmod.handler.ServerEventHandler;
import com.extrahelden.duelmod.helper.Helper;
import com.extrahelden.duelmod.network.NetworkHandler;
import com.extrahelden.duelmod.perms.Permissions;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.Mth;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class ShowLivesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("mylives")
                .requires(src -> Permissions.check(src, Permissions.MYLIVES_BASE))

                // /mylives set <targets> <lives>
                .then(Commands.literal("set")
                        .requires(src -> Permissions.check(src, Permissions.MYLIVES_SET))
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .then(Commands.argument("lives", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> {

                                            CommandSourceStack src = ctx.getSource();
                                            MinecraftServer server = src.getServer();
                                            GameProfileCache cache = server.getProfileCache();

                                            Collection<GameProfile> targets =
                                                    GameProfileArgument.getGameProfiles(ctx, "targets");

                                            int lives = Mth.clamp(
                                                    IntegerArgumentType.getInteger(ctx, "lives"),
                                                    0, 99
                                            );

                                            int ok = 0;

                                            for (GameProfile gpIn : targets) {
                                                GameProfile gp = gpIn;

                                                if ((gp.getId() == null || gp.getName() == null)
                                                        && gpIn.getName() != null) {
                                                    Optional<GameProfile> fromCache = cache.get(gpIn.getName());
                                                    if (fromCache.isPresent()) gp = fromCache.get();
                                                }

                                                String name = gp.getName() != null ? gp.getName() : "(unbekannt)";
                                                UUID uuid = gp.getId();

                                                if (uuid == null) {
                                                    src.sendFailure(Component.literal(
                                                            Helper.getPrefix() +
                                                                    " §cSpieler §e" + name +
                                                                    "§c ist nicht im Server-Cache."
                                                    ));
                                                    continue;
                                                }

                                                // 1) Offline-Persistenz
                                                ServerEventHandler.saveMyLivesData(uuid, lives);

                                                // 2) Online → sofort NBT + Client-Sync
                                                ServerPlayer online =
                                                        server.getPlayerList().getPlayer(uuid);

                                                if (online != null) {
                                                    online.getPersistentData()
                                                            .putInt("MyLives", lives);

                                                    OwnerPair owner =
                                                            readOrResolveOwner(online, server);

                                                    // 🔥 FIX: SOFORT CLIENT SYNC
                                                    NetworkHandler.syncLives(
                                                            online,
                                                            lives,
                                                            owner.name,
                                                            owner.uuid
                                                    );

                                                    online.sendSystemMessage(Component.literal(
                                                            Helper.getPrefix() +
                                                                    " §7Deine Leben wurden auf §b" +
                                                                    lives + "§7 gesetzt."
                                                    ));
                                                }

                                                src.sendSuccess(() -> Component.literal(
                                                        Helper.getPrefix() +
                                                                " §aLeben von §e" + name +
                                                                " §a→ §b" + lives
                                                ), true);

                                                ok++;
                                            }

                                            return ok;
                                        })
                                )
                        )
                )

                // /mylives setname <name> <lives>
                .then(Commands.literal("setname")
                        .requires(src -> Permissions.check(src, Permissions.MYLIVES_SETNAME))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("lives", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> {

                                            CommandSourceStack src = ctx.getSource();
                                            MinecraftServer server = src.getServer();
                                            GameProfileCache cache = server.getProfileCache();

                                            String name = StringArgumentType.getString(ctx, "name");
                                            int lives = Mth.clamp(
                                                    IntegerArgumentType.getInteger(ctx, "lives"),
                                                    0, 99
                                            );

                                            Optional<GameProfile> profOpt = cache.get(name);
                                            if (profOpt.isEmpty()) {
                                                src.sendFailure(Component.literal(
                                                        Helper.getPrefix() +
                                                                " §cSpieler §e" + name +
                                                                "§c ist nicht im Server-Cache."
                                                ));
                                                return 0;
                                            }

                                            GameProfile prof = profOpt.get();
                                            UUID uuid = prof.getId();

                                            ServerEventHandler.saveMyLivesData(uuid, lives);

                                            ServerPlayer online =
                                                    server.getPlayerList().getPlayer(uuid);

                                            if (online != null) {
                                                online.getPersistentData()
                                                        .putInt("MyLives", lives);

                                                OwnerPair owner =
                                                        readOrResolveOwner(online, server);

                                                NetworkHandler.syncLives(
                                                        online,
                                                        lives,
                                                        owner.name,
                                                        owner.uuid
                                                );

                                                online.sendSystemMessage(Component.literal(
                                                        Helper.getPrefix() +
                                                                " §7Deine Leben wurden auf §b" +
                                                                lives + "§7 gesetzt."
                                                ));
                                            }

                                            src.sendSuccess(() -> Component.literal(
                                                    Helper.getPrefix() +
                                                            " §aLeben von §e" + name +
                                                            " §a→ §b" + lives
                                            ), true);

                                            return 1;
                                        })
                                )
                        )
                )
        );
    }

    // ----------------------------------------------------

    private record OwnerPair(String name, String uuid) {}

    private static OwnerPair readOrResolveOwner(ServerPlayer player, MinecraftServer server) {
        var data = player.getPersistentData();

        String ownerName = data.getString("LinkedHeartOwner");
        if (ownerName == null || ownerName.isBlank()) {
            ownerName = player.getGameProfile().getName();
            data.putString("LinkedHeartOwner", ownerName);
        }

        String ownerUuid = data.getString("LinkedHeartOwnerUUID");
        if (ownerUuid == null || ownerUuid.isBlank()) {
            ownerUuid = resolveUuidByName(server, ownerName);
            data.putString("LinkedHeartOwnerUUID", ownerUuid);
        }

        return new OwnerPair(ownerName, ownerUuid);
    }

    private static String resolveUuidByName(MinecraftServer server, String name) {
        if (name == null || name.isBlank()) return "";

        if (server != null) {
            Optional<GameProfile> opt = server.getProfileCache().get(name);
            if (opt.isPresent() && opt.get().getId() != null) {
                return opt.get().getId().toString();
            }
        }

        UUID off = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + name)
                        .getBytes(StandardCharsets.UTF_8)
        );
        return off.toString();
    }
}
