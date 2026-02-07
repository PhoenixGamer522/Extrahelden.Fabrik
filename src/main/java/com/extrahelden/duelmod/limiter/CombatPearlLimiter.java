package com.extrahelden.duelmod.limiter;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.combat.CombatManager;
import com.extrahelden.duelmod.helper.Helper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DuelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatPearlLimiter {

    private static final Map<UUID, Integer> pearlUses = new HashMap<>();
    private static final Map<UUID, Integer> pearlTimers = new HashMap<>();
    private static final int MAX_USES = 48; // = 3 Stacks (3 * 16)

    // ----------------- Enderpearl-Use Event -----------------
    @SubscribeEvent
    public static void onEnderpearlUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getItemStack().getItem() != Items.ENDER_PEARL) return;

        // Nur im Combat limitieren
        if (!CombatManager.isInCombat(player)) return;

        UUID id = player.getUUID();
        int used = pearlUses.getOrDefault(id, 0);

        if (used >= MAX_USES) {
            event.setCanceled(true);
            player.sendSystemMessage(
                    Component.literal(Helper.getPrefix() + "§cDu hast das Limit von 3 Stacks Enderperlen in diesem Kampf erreicht!")
            );
            return;
        }

        // Erlaubt → +1
        used++;
        pearlUses.put(id, used);

        // PearlTimer an Combat anpassen
        pearlTimers.put(id, CombatManager.getRemainingTicks(player));

        int remaining = MAX_USES - used;
        player.sendSystemMessage(
                Component.literal(Helper.getPrefix() + "§7Enderperlen übrig in diesem Kampf: §e" + remaining)
        );
    }

    // ----------------- Timer Tick (mit Combat synchronisiert) -----------------
    public static void tick() {
        Iterator<Map.Entry<UUID, Integer>> it = pearlTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            UUID id = entry.getKey();
            int ticks = entry.getValue() - 1;

            if (ticks <= 0) {
                pearlUses.remove(id);
                it.remove(); // Timer abgelaufen → entfernen
            } else {
                entry.setValue(ticks);
            }
        }
    }

    // ----------------- Reset -----------------
    public static void resetPearlUses(ServerPlayer player) {
        resetPearlUses(player.getUUID());
    }

    public static void resetPearlUses(UUID id) {
        pearlUses.remove(id);
        pearlTimers.remove(id);
    }
}
