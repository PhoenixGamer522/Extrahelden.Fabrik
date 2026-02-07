package com.extrahelden.duelmod.handler;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.mobs.DummyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    // Deferred register für EntityTypes, registriert unter unserer ModID
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DuelMod.MOD_ID);

    // CustomZombie-Entity

    // DummyEntity-Entity
    public static final RegistryObject<EntityType<DummyEntity>> DUMMY_ENTITY =
            ENTITY_TYPES.register("dummy_entity", () ->
                    EntityType.Builder.<DummyEntity>of(DummyEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .build("dummy_entity")
            );

    /** Muss in der Mod-Hauptklasse aufgerufen werden, z.B.:
     ModEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
     */
    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    /** Registriert Attribute (Health, Speed, etc.) für unsere Entities */
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        // EventZombie benutzt seine eigene createAttributes(
        // DummyEntity kann die Standard-Zombie-Attribute übernehmen
        AttributeSupplier dummyAttrs = Zombie.createAttributes().build();
        event.put(DUMMY_ENTITY.get(), dummyAttrs);
    }
}

