package com.ourmagic.magic.spell.modifier;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.effect.ChainStart;
import com.ourmagic.magic.spell.effect.ChainableEffect;
import com.ourmagic.magic.spell.effect.DelayedSpellCasts;
import com.ourmagic.magic.spell.effect.SpellContext;
import com.ourmagic.magic.spell.effect.SpellEffect;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class MultistrikeModifier implements SpellModifier {
    private static final int RECAST_DELAY_TICKS = 6;

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
            float power = 1.0F;
            boolean cast = effect.cast(context.withCastIteration(0, casts, power));
            if (!(context.level() instanceof ServerLevel serverLevel)) {
                return cast;
            }

            for (int i = 1; i < casts; i++) {
                int castIndex = i;
                DelayedSpellCasts.schedule(serverLevel, RECAST_DELAY_TICKS * castIndex, () ->
                        effect.cast(context.withCastIteration(castIndex, casts, power)));
            }
            return cast;
    }
}
