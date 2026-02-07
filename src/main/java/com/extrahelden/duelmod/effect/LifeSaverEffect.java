package com.extrahelden.duelmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ein einfacher Effekt, der als Lebensretter dient.
 */
public class LifeSaverEffect extends MobEffect {

    public LifeSaverEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Default-Verhalten beibehalten
        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Standard-Takt-Logik verwenden
        return super.isDurationEffectTick(duration, amplifier);
    }
}
