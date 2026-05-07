package com.ourmagic.magic.spell.runtime;

import com.ourmagic.magic.spell.shapes.*;

import com.ourmagic.magic.spell.payloads.PayloadEffect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class ComposedSpellEffect implements ChainableEffect {
    private final TargetSelector targetSelector;
    private final AreaSelector areaSelector;
    private final PayloadEffect payload;
    private final Optional<ParticleOptions> areaRingParticle;
    private final ParticleOptions chainParticle;

    public ComposedSpellEffect(SpellShape shape, PayloadEffect payload) {
        this(shape, payload, ParticleTypes.ENCHANT);
    }

    public ComposedSpellEffect(SpellShape shape, PayloadEffect payload, ParticleOptions chainParticle) {
        this(shape.targetSelector(), shape.areaSelector(), payload, shape.areaRingParticle().orElse(null), chainParticle);
    }

    public ComposedSpellEffect(TargetSelector targetSelector, AreaSelector areaSelector, PayloadEffect payload, ParticleOptions areaRingParticle) {
        this(targetSelector, areaSelector, payload, areaRingParticle, ParticleTypes.ENCHANT);
    }

    public ComposedSpellEffect(TargetSelector targetSelector, AreaSelector areaSelector, PayloadEffect payload, ParticleOptions areaRingParticle, ParticleOptions chainParticle) {
        this.targetSelector = targetSelector;
        this.areaSelector = areaSelector;
        this.payload = payload;
        this.areaRingParticle = Optional.ofNullable(areaRingParticle);
        this.chainParticle = chainParticle;
    }

    @Override
    public boolean cast(SpellContext context) {
        return targetSelector.select(context)
                .map(origin -> {
                    double radius = areaSelector.radius(context);
                    if (radius > 0.0D) {
                        if (origin.entity().map(entity -> entity != context.player()).orElse(true)) {
                            context.beam(origin.position(), chainParticle);
                        }
                        areaRingParticle.ifPresent(particle -> context.ring(origin.position(), particle, radius, 36));
                    }

                    List<SpellTarget> targets = areaSelector.select(context, origin);
                    SpellContext payloadContext = radius > 0.0D ? context.withoutBeams().asAreaCast(origin.position()) : context;
                    boolean applied = false;
                    for (SpellTarget target : targets) {
                        applied |= payload.apply(payloadContext, target);
                    }
                    return applied;
                })
                .orElse(false);
    }

    @Override
    public Optional<ChainStart> chainStart(SpellContext context) {
        return targetSelector.select(context).map(target -> new ChainStart(
                target.position(),
                target.entity().map(entity -> entity.getId()).orElse(-1)
        ));
    }

    @Override
    public ParticleOptions chainParticle() {
        return chainParticle;
    }

    @Override
    public void applyChainTarget(SpellContext context, LivingEntity target, Vec3 targetPos, float powerMultiplier) {
        payload.apply(context.withCastIteration(context.castIndex(), context.castCount(), context.modifierPower() * powerMultiplier), SpellTarget.living(target));
    }
}
