package com.extrahelden.duelmod.events;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.combat.CombatManager;
import com.extrahelden.duelmod.limiter.CombatPearlLimiter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DuelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        CombatManager.tick();
        CombatPearlLimiter.tick();
    }
}
