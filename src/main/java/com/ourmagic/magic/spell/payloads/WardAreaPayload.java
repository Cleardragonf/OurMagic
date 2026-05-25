package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public class WardAreaPayload implements PayloadEffect {
    private final Variant variant;

    public WardAreaPayload(Variant variant) {
        this.variant = variant;
    }

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return switch (variant) {
            case ANTI_FIRE -> ParticleTypes.SMOKE;
            case ANTI_WATER -> ParticleTypes.BUBBLE;
            case ANTI_EXPLOSION -> ParticleTypes.POOF;
            case ANTI_MAGIC -> ParticleTypes.ENCHANT;
            case ANTI_GRIEF -> ParticleTypes.CRIT;
            case ANTI_PROJECTILE -> ParticleTypes.SWEEP_ATTACK;
            case ANTI_TELEPORT -> ParticleTypes.REVERSE_PORTAL;
            case ANTI_DECAY -> ParticleTypes.COMPOSTER;
            case ANTI_SUMMON -> ParticleTypes.SOUL;
            case AIR -> ParticleTypes.CLOUD;
            case AIR_BUBBLE -> ParticleTypes.BUBBLE;
            case ALARM -> ParticleTypes.NOTE;
            case ENTRY_FILTER -> ParticleTypes.END_ROD;
            case WEATHER -> ParticleTypes.CLOUD;
            case LOCKDOWN -> ParticleTypes.ENCHANT;
            case ITEM_GUARD -> ParticleTypes.WAX_ON;
            case STORAGE_LOCK -> ParticleTypes.WAX_ON;
            case GROW -> ParticleTypes.HAPPY_VILLAGER;
            case FREEZE -> ParticleTypes.SNOWFLAKE;
            case THAW -> ParticleTypes.SPLASH;
            case WEAKENING -> ParticleTypes.ASH;
            case MANA_DRAIN -> ParticleTypes.WITCH;
            case REFLECT_PROJECTILE -> ParticleTypes.FLASH;
            case DISPEL -> ParticleTypes.REVERSE_PORTAL;
            case LIGHT -> ParticleTypes.GLOW;
            case DARK -> ParticleTypes.SQUID_INK;
            case TEMPORAL -> ParticleTypes.PORTAL;
            case STASIS -> ParticleTypes.ENCHANT;
            case FERTILITY -> ParticleTypes.HAPPY_VILLAGER;
            case CAMOUFLAGE -> ParticleTypes.SPORE_BLOSSOM_AIR;
        };
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        return false;
    }

    public enum Variant {
        ANTI_FIRE,
        ANTI_WATER,
        ANTI_EXPLOSION,
        ANTI_MAGIC,
        ANTI_GRIEF,
        ANTI_PROJECTILE,
        ANTI_TELEPORT,
        ANTI_DECAY,
        ANTI_SUMMON,
        AIR,
        AIR_BUBBLE,
        ALARM,
        ENTRY_FILTER,
        WEATHER,
        LOCKDOWN,
        ITEM_GUARD,
        STORAGE_LOCK,
        GROW,
        FREEZE,
        THAW,
        WEAKENING,
        MANA_DRAIN,
        REFLECT_PROJECTILE,
        DISPEL,
        LIGHT,
        DARK,
        TEMPORAL,
        STASIS,
        FERTILITY,
        CAMOUFLAGE
    }
}
