package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RecallPayload implements PayloadEffect {
    private static final Map<UUID, RecallMark> MARKS = new HashMap<>();

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.REVERSE_PORTAL;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RANGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        ServerPlayer player = context.player();
        RecallMark mark = MARKS.remove(player.getUUID());
        if (mark == null) {
            MARKS.put(player.getUUID(), new RecallMark(player.level().dimension(), player.position(), player.getYRot(), player.getXRot()));
            context.ring(player.position().add(0, 0.1D, 0), ParticleTypes.REVERSE_PORTAL, 1.0D, 36);
            context.burst(player.position().add(0, 0.6D, 0), ParticleTypes.ENCHANT, 24, 0.45D, 0.03D);
            return true;
        }

        ServerLevel destination = player.serverLevel().getServer().getLevel(mark.dimension());
        if (destination == null) {
            return false;
        }

        context.burst(player.position().add(0, 0.8D, 0), ParticleTypes.PORTAL, 70, 0.65D, 0.12D);
        player.teleportTo(destination, mark.position().x, mark.position().y, mark.position().z, mark.yaw(), mark.pitch());
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, mark.position().x, mark.position().y + 0.8D, mark.position().z, 70, 0.65D, 0.65D, 0.65D, 0.08D);
        }
        return true;
    }

    private record RecallMark(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
    }
}
