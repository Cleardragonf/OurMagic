package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicDamageSources;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import com.ourmagic.mana.PlayerMana;
import com.ourmagic.network.ModNetwork;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class ManaBurnPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DAMAGE, Spell.UPGRADE_RANGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        if (living instanceof ServerPlayer player) {
            PlayerMana mana = PlayerMana.get(player);
            mana.spend(Math.max(6, Math.round(12 * context.damagePower())));
            ModNetwork.syncMana(player, mana);
        } else {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.round(120 * context.durationMultiplier()), 1));
        }
        living.hurt(MagicDamageSources.playerMagic(context.level(), context.player()), 2.0F * context.damagePower());
        context.beam(target.position(), ParticleTypes.SOUL_FIRE_FLAME);
        context.burst(living.position().add(0, living.getBbHeight() * 0.55D, 0), ParticleTypes.SOUL_FIRE_FLAME, 24, 0.45D, 0.04D);
        return true;
    }
}
