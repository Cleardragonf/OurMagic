package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class HealPayload implements PayloadEffect {
    private final float selfAmount;
    private final float otherAmount;
    private final int particleCount;

    public HealPayload(float amount, int particleCount) {
        this(amount, amount, particleCount);
    }

    public HealPayload(float selfAmount, float otherAmount, int particleCount) {
        this.selfAmount = selfAmount;
        this.otherAmount = otherAmount;
        this.particleCount = particleCount;
    }

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RANGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        float amount = living == context.player() ? selfAmount : otherAmount;
        living.heal(amount * context.utilityPower());
        context.burst(living.position().add(0, 1.1D, 0), ParticleTypes.HEART, particleCount, 0.55D, 0.02D);
        return true;
    }
}
