package com.ourmagic.magic.spell.payloads;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public class IllusionPayload implements PayloadEffect {
    private static final List<Decoy> DECOYS = new ArrayList<>();

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.POOF;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_RADIUS, Spell.UPGRADE_RANGE, Spell.UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }

        Vec3 center = target.entity().map(entity -> entity.position()).orElse(target.position());
        double radius = 5.0D * context.radiusMultiplier();
        int duration = Math.round(90 * context.durationMultiplier() * context.utilityPower());
        ArmorStand decoy = createDecoy(level, context.player(), center, duration);
        if (decoy == null) {
            return false;
        }

        for (Mob mob : context.level().getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(radius), mob -> mob.isAlive())) {
            mob.setTarget(null);
            mob.setTarget(decoy);
            mob.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        }

        context.burst(center, ParticleTypes.POOF, 45, Math.min(1.5D, radius * 0.25D), 0.05D);
        context.burst(center, ParticleTypes.WITCH, 18, Math.min(1.1D, radius * 0.18D), 0.02D);
        return true;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DECOYS.isEmpty()) {
            return;
        }

        Iterator<Decoy> iterator = DECOYS.iterator();
        while (iterator.hasNext()) {
            Decoy decoy = iterator.next();
            Entity entity = decoy.level.getEntity(decoy.entity);
            if (entity == null || !entity.isAlive() || decoy.ticks-- <= 0) {
                if (entity != null) {
                    decoy.level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 20, 0.35D, 0.45D, 0.35D, 0.03D);
                    entity.discard();
                }
                iterator.remove();
            } else if (entity.tickCount % 10 == 0) {
                decoy.level.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 5, 0.25D, 0.35D, 0.25D, 0.01D);
            }
        }
    }

    private static ArmorStand createDecoy(ServerLevel level, ServerPlayer player, Vec3 position, int duration) {
        ArmorStand decoy = EntityType.ARMOR_STAND.create(level);
        if (decoy == null) {
            return null;
        }

        decoy.moveTo(position.x, position.y, position.z, player.getYRot(), player.getXRot());
        decoy.setCustomName(player.getDisplayName());
        decoy.setCustomNameVisible(true);
        decoy.setShowArms(true);
        decoy.setNoBasePlate(true);
        decoy.setInvulnerable(false);
        decoy.setItemSlot(EquipmentSlot.HEAD, playerHead(player));
        decoy.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        decoy.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        decoy.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());
        decoy.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
        decoy.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem().copy());
        level.addFreshEntity(decoy);
        DECOYS.add(new Decoy(level, decoy.getUUID(), duration));
        return decoy;
    }

    private static ItemStack playerHead(ServerPlayer player) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag tag = head.getOrCreateTag();
        tag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), player.getGameProfile()));
        return head;
    }

    private static final class Decoy {
        private final ServerLevel level;
        private final UUID entity;
        private int ticks;

        private Decoy(ServerLevel level, UUID entity, int ticks) {
            this.level = level;
            this.entity = entity;
            this.ticks = ticks;
        }
    }
}
