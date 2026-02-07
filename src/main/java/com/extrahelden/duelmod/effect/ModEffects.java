package com.extrahelden.duelmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS;
    public static final RegistryObject<MobEffect> LIFESAVER_EFFECT;

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    static {
        MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "minecrafthelden");
        LIFESAVER_EFFECT = MOB_EFFECTS.register("lifesaver", () -> new LifeSaverEffect(MobEffectCategory.NEUTRAL, 8388863));
    }
}
