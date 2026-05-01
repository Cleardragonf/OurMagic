package com.ourmagic.magic.spell.modifier;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.effect.ChainStart;
import com.ourmagic.magic.spell.effect.ChainableEffect;
import com.ourmagic.magic.spell.effect.SpellContext;
import com.ourmagic.magic.spell.effect.SpellEffect;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class MultistrikeModifier implements SpellModifier {
    @Override
    public SpellEffect wrap(Spell spell, SpellEffect effect) {
        if (!spell.supportsUpgradeFamily(Spell.UPGRADE_MULTISTRIKE)) {
            return effect;
        }

        if (effect instanceof ChainableEffect chainable) {
            return new ChainableEffect() {
                @Override
                public boolean cast(SpellContext context) {
                    return castRepeated(context, effect);
                }

                @Override
                public Optional<ChainStart> chainStart(SpellContext context) {
                    return chainable.chainStart(context);
                }

                @Override
                public ParticleOptions chainParticle() {
                    return chainable.chainParticle();
                }

                @Override
                public void applyChainTarget(SpellContext context, LivingEntity target, Vec3 targetPos, float powerMultiplier) {
                    chainable.applyChainTarget(context, target, targetPos, powerMultiplier);
                }
            };
        }

        return context -> castRepeated(context, effect);
    }

    private static boolean castRepeated(SpellContext context, SpellEffect effect) {
            int casts = 1 + context.data().activeUpgradeLevel(Spell.UPGRADE_MULTISTRIKE) + context.data().activeUpgradeLevel(Spell.UPGRADE_CASTS);
            float power = 1.0F + context.data().activeUpgradeLevel(Spell.UPGRADE_DAMAGE) * 0.08F;
            boolean cast = false;
            for (int i = 0; i < casts; i++) {
                cast |= effect.cast(context.withCastIteration(i, casts, power));
            }
            return cast;
    }
}
