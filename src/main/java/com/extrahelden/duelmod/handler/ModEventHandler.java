package com.extrahelden.duelmod.handler;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.combat.CombatManager;
import com.github.alexthe666.alexsmobs.entity.util.FlyingFishBootsUtil;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DuelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEventHandler {

    /* ------------------------------------------------------------
       ZENTRALER COMBAT-BLOCK (Riptide + Fish Boots)
       ------------------------------------------------------------ */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!CombatManager.isInCombat(sp)) return;

        Player player = sp;

    /* ------------------------------------------------------------
       Riptide komplett blockieren
       ------------------------------------------------------------ */
        if (isHoldingRiptideTrident(player)) {
            killHorizontalMovement(player);
            player.stopUsingItem();
            player.getCooldowns().addCooldown(Items.TRIDENT, 20);

            sendMessageThrottled(sp);
        }

    /* ------------------------------------------------------------
       Flying Fish Boots – ENDGÜLTIGER BLOCK
       ------------------------------------------------------------ */
        if (FlyingFishBootsUtil.isWearing(player)) {

            // Alex's Mobs Boost IMMER resetten
            FlyingFishBootsUtil.setBoostTicks(player, 0);

            // Wenn im Wasser → Swim-Speed hart begrenzen
            if (player.isInWaterOrBubble()) {
                limitWaterMovement(player);
            } else {
                // an Land / Luft: komplett stoppen
                killHorizontalMovement(player);
            }

            // Cooldown auf Boots setzen
            ItemStack boots = player.getInventory().armor.get(0); // FEET
            if (!boots.isEmpty()) {
                player.getCooldowns().addCooldown(boots.getItem(), 20);
            }

            sendMessageThrottled(sp);
        }
    }

    /* ------------------------------------------------------------
       Squid Grapple blockieren (Alex's Mobs)
       ------------------------------------------------------------ */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!CombatManager.isInCombat(sp)) return;

        ItemStack stack = event.getItemStack();

        if (stack.getItem() == AMItemRegistry.SQUID_GRAPPLE.get()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            sp.displayClientMessage(
                    Component.literal("Das kannst du im Kampf nicht benutzen!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    /* ------------------------------------------------------------
       Helper
       ------------------------------------------------------------ */
    private static void killHorizontalMovement(Player player) {
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
        player.hurtMarked = true;
    }

    private static boolean isHoldingRiptideTrident(Player player) {
        return isRiptide(player.getMainHandItem()) || isRiptide(player.getOffhandItem());
    }

    private static boolean isRiptide(ItemStack stack) {
        return stack.getItem() == Items.TRIDENT &&
                EnchantmentHelper.getItemEnchantmentLevel(Enchantments.RIPTIDE, stack) > 0;
    }

    private static void sendMessageThrottled(ServerPlayer sp) {
        // Nachricht max. alle 10 Ticks
        if (sp.tickCount % 10 != 0) return;

        sp.displayClientMessage(
                Component.literal("Das kannst du im Kampf nicht benutzen!")
                        .withStyle(ChatFormatting.RED),
                true
        );
    }
    private static void limitWaterMovement(Player player) {
        var vel = player.getDeltaMovement();

        double max = 0.03D; // Vanilla Swim-Speed
        double clampedX = Math.max(-max, Math.min(max, vel.x));
        double clampedZ = Math.max(-max, Math.min(max, vel.z));

        player.setDeltaMovement(clampedX, vel.y, clampedZ);
        player.hurtMarked = true;
    }
}

