package com.ourmagic.magic.spell.modifier;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.effect.ChainStart;
import com.ourmagic.magic.spell.effect.ChainableEffect;
import com.ourmagic.magic.spell.effect.SpellEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ChainingModifier implements SpellModifier {
    @Override
    public SpellEffect wrap(Spell spell, SpellEffect effect) {
        if (!spell.supportsUpgradeFamily(Spell.UPGRADE_CHAINING) || !(effect instanceof ChainableEffect chainable)) {
            return effect;
        }

        return context -> {
            boolean cast = effect.cast(context);
            int chainLevel = context.data().activeUpgradeLevel(Spell.UPGRADE_CHAINING) + context.data().activeUpgradeLevel(Spell.UPGRADE_CHAINING_ENTITIES);
            if (!cast || chainLevel <= 0 || !(context.level() instanceof ServerLevel serverLevel)) {
                return cast;
            }

            Optional<ChainStart> start = chainable.chainStart(context);
            if (start.isEmpty()) {
                return cast;
            }

            Vec3 origin = start.get().origin();
            Set<Integer> hitEntities = new HashSet<>();
            if (start.get().excludedEntityId() >= 0) {
                hitEntities.add(start.get().excludedEntityId());
            }

            double radius = 4.0D + chainLevel + context.data().activeUpgradeLevel(Spell.UPGRADE_CHAINING_RADIUS);
            float chainPower = 1.0F + (1 + context.data().activeUpgradeLevel(Spell.UPGRADE_CHAINING_DAMAGE)) * 0.10F;

            for (int i = 0; i < chainLevel; i++) {
                Optional<LivingEntity> next = context.nearestLiving(origin, radius, hitEntities);
                if (next.isEmpty()) {
                    return cast;
                }

                LivingEntity target = next.get();
                hitEntities.add(target.getId());
                Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5D, 0);
                serverLevel.sendParticles(chainable.chainParticle(), origin.x, origin.y, origin.z, 10, 0.18D, 0.18D, 0.18D, 0.03D);
                serverLevel.sendParticles(chainable.chainParticle(), targetPos.x, targetPos.y, targetPos.z, 18, 0.35D, 0.35D, 0.35D, 0.02D);
                chainable.applyChainTarget(context, target, targetPos, chainPower);
                origin = targetPos;
            }

            return cast;
        };
    }
}
