package com.extrahelden.duelmod.item;

import com.extrahelden.duelmod.DuelMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DuelMod.MOD_ID);

    public static final RegistryObject<Item> HEART_STEALER =
            ITEMS.register("heart_stealer",
                    () -> new HeartStealerItem(new Item.Properties()));

}