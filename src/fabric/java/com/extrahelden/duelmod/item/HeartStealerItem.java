package com.extrahelden.duelmod.item;

import com.extrahelden.duelmod.state.PlayerState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class HeartStealerItem extends Item {
    public HeartStealerItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof ServerPlayerEntity victim && attacker instanceof ServerPlayerEntity player) {
            int current = PlayerState.getLives(victim);
            if (current > 0) {
                PlayerState.addLives(victim, -1);
                PlayerState.addLives(player, 1);
                victim.sendMessage(Text.literal("Du hast 1 Leben verloren."), false);
                player.sendMessage(Text.literal("Du hast 1 Leben gestohlen."), false);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
}
