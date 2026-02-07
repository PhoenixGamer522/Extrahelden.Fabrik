package com.extrahelden.duelmod.item;

import com.extrahelden.duelmod.helper.Helper;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class HeartStealerItem extends Item {

    public HeartStealerItem(Properties props) {
        super(props.stacksTo(1).fireResistant());
    }
    /**
     * Keine Attribute anzeigen (kein Schaden, keine Speed-Werte)
     */
    @Override
    public ImmutableMultimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return ImmutableMultimap.of(); // Leere Map → keine grünen Attribute
    }

    /**
     * Tooltip mit Beschreibung
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§cStiehlt beim Treffer ein Extra Herz"));
    }

    /**
     * Wird aufgerufen, wenn ein Spieler ein Entity angreift.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayer player)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        if (target instanceof ServerPlayer victim) {
            var data = victim.getPersistentData();
            int lives = data.getInt("MyLives");

            if (lives > 0) {
                int newLives = Math.max(0, lives - 1);
                data.putInt("MyLives", newLives);

                // Nachricht an beide Spieler
                victim.sendSystemMessage(Component.literal(
                        Helper.getPrefix() + "§c Dir wurde ein Extra Herz gestohlen!"
                ));
                player.sendSystemMessage(Component.literal(
                        Helper.getPrefix() + "§a Du hast ein Herz von " + victim.getName().getString() + " gestohlen!"
                ));

                // Item danach zerstören
                stack.shrink(1);
            }
        }

        return true; // kein normaler Schwertschaden
    }
}
