package com.extrahelden.duelmod.item;

import com.extrahelden.duelmod.DuelModFabric;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item HEART_STEALER = Registry.register(
            Registries.ITEM,
            new Identifier(DuelModFabric.MOD_ID, "heart_stealer"),
            new HeartStealerItem(new Item.Settings().maxCount(1))
    );

    private ModItems() {
    }

    public static void register() {
        // static init
    }
}
