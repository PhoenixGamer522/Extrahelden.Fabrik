package com.extrahelden.duelmod;

import com.extrahelden.duelmod.command.LivesCommand;
import com.extrahelden.duelmod.item.ModItems;
import com.extrahelden.duelmod.state.PlayerState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuelModFabric implements ModInitializer {
    public static final String MOD_ID = "duelmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> LivesCommand.register(dispatcher));

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                PlayerState.setLives(newPlayer, PlayerState.getLives(oldPlayer))
        );

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && PlayerState.getLives(newPlayer) <= 0) {
                newPlayer.sendMessage(net.minecraft.text.Text.literal("Du hast keine Leben mehr."), false);
            }
        });

        LOGGER.info("DuelMod wurde als Fabric-Mod initialisiert.");
    }
}
