package com.ourmagic.item;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.mana.PlayerMana;
import com.ourmagic.network.CraftSpellPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.StaffData;
import com.ourmagic.wand.WandModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StaffItem extends Item {
    private static final int SPELL_TOOLTIP_WINDOW = 8;

    public StaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StaffData data = StaffData.read(stack);

        if (tryAddSpellSource(level, player, hand, stack, data)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                var players = level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(20));
                for (var p : players) {
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(data.displayName() + " - Level " + data.staffLevel())));
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (data.activeSpell().isBlank()) {
            return InteractionResultHolder.pass(stack);
        }

        if (castSpell(level, player, hand, stack, data)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        StaffData data = StaffData.read(stack);

        lines.add(Component.literal("§6Level: " + data.staffLevel() + "/" + 20));
        lines.add(Component.literal("§6XP: " + data.staffXp() + "/" + data.staffXpToNextLevel()));
        lines.add(Component.literal("§6Modifier Slots: " + data.usedStaffModifierSlots() + "/" + data.staffModifierSlots()));
        lines.add(Component.literal(""));
        lines.add(Component.literal("§9Active Spell: §f" + data.activeSpellName()));

        int spellCount = data.spells().size();
        if (spellCount > 0) {
            lines.add(Component.literal("§7Spells (" + spellCount + "):"));
            for (int i = 0; i < Math.min(spellCount, SPELL_TOOLTIP_WINDOW); i++) {
                var spell = data.spells().get(i);
                lines.add(Component.literal("  §8- " + spell.displayName() + " L" + spell.level()));
            }
            if (spellCount > SPELL_TOOLTIP_WINDOW) {
                lines.add(Component.literal("  §8+ " + (spellCount - SPELL_TOOLTIP_WINDOW) + " more"));
            }
        }

        lines.add(Component.literal(""));
        lines.add(Component.literal("§7Modifiers:"));
        if (data.modifiers().isEmpty()) {
            lines.add(Component.literal("  §8None"));
        } else {
            for (var entry : data.modifiers().entrySet()) {
                lines.add(Component.literal("  §8- " + entry.getKey() + " Lvl" + entry.getValue()));
            }
        }

        lines.add(Component.literal(""));
        lines.add(Component.literal("§5Melee: " + String.format("%.1f", data.meleeDamage()) + " damage"));
        lines.add(Component.literal("§5Spell: " + data.activeManaCost() + " mana / " + (data.activeCooldownTicks() / 20.0) + "s cooldown"));
    }

    private boolean castSpell(Level level, Player player, InteractionHand hand, ItemStack stack, StaffData data) {
        Spell spell = SpellRegistry.get(data.activeSpell());
        if (spell == null) {
            return false;
        }

        if (data.cooldownUntil() > level.getGameTime()) {
            return false;
        }

        PlayerMana mana = PlayerMana.get(player);
        int manaCost = data.activeManaCost();
        int cooldownTicks = data.activeCooldownTicks();
        if (!player.getAbilities().instabuild && !mana.has(manaCost)) {
            player.displayClientMessage(Component.translatable("message.ourmagic.no_mana").withStyle(ChatFormatting.RED), true);
            return false;
        }

        float chantMultiplier = player.hasEffect(MagicStatusEffects.CHANT.get()) ? 1.5F : 1.0F;
        boolean cast = spell.cast(level, player, stack, data, chantMultiplier);
        if (cast) {
            if (!player.getAbilities().instabuild) {
                mana.spend(manaCost);
                ModNetwork.syncMana(player, mana);
            }
            int xp = Math.max(1, Math.round(5 * data.xpMultiplier()));
            int levelsGained = data.addActiveSpellXp(xp);
            if (levelsGained > 0) {
                celebrateSpellLevelUp(player, data.activeSpellName());
            }
            int staffLevelsGained = data.addStaffXp(xp);
            if (staffLevelsGained > 0) {
                player.displayClientMessage(Component.literal("Your staff reached level " + data.staffLevel() + ".").withStyle(ChatFormatting.GOLD), true);
            }
            data.setCooldownUntil(level.getGameTime() + cooldownTicks);
            data.save(stack);
            player.getInventory().setChanged();
        }

        return cast;
    }

    public boolean swingAsWeapon(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return true;
        }

        StaffData data = StaffData.read(stack);
        int xp = Math.max(1, Math.round(5 * data.xpMultiplier()));
        int staffLevelsGained = data.addStaffXp(xp);
        if (staffLevelsGained > 0) {
            player.displayClientMessage(Component.literal("Your staff reached level " + data.staffLevel() + ".").withStyle(ChatFormatting.GOLD), true);
        }
        data.save(stack);
        player.getInventory().setChanged();

        return true;
    }

    private static void celebrateSpellLevelUp(Player player, String spellName) {
        player.displayClientMessage(
                Component.literal("§6✨ " + spellName + " leveled up! ✨").withStyle(ChatFormatting.GOLD),
                true
        );
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.LEVEL_UP,
                SoundSource.NEUTRAL,
                0.5F,
                0.5F
        );
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            for (int i = 0; i < 10; i++) {
                ((ServerLevel) player.level()).sendParticles(
                        ParticleTypes.SOUL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        1, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2,
                        1.0
                );
            }
        });
    }

    private static boolean tryAddSpellSource(Level level, Player player, InteractionHand staffHand, ItemStack staff, StaffData data) {
        InteractionHand sourceHand = staffHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack source = player.getItemInHand(sourceHand);
        boolean grimoire = source.is(ModItems.GRIMOIRE.get()) && GrimoireItem.hasSelectedSpell(source);
        if (!grimoire && !WandItem.isSpellPaper(source)) {
            return false;
        }

        if (level.isClientSide) {
            return true;
        }

        SpellInstance sourceSpell = grimoire ? GrimoireItem.selectedSpellInstance(source) : SpellInstance.fromItem(source);
        if (sourceSpell == null) {
            return false;
        }

        if (data.addSpell(sourceSpell)) {
            player.displayClientMessage(
                    Component.translatable("message.ourmagic.added_spell", sourceSpell.displayName()),
                    true
            );
            data.save(staff);
            return true;
        }

        player.displayClientMessage(
                Component.translatable("message.ourmagic.spell_already_added", sourceSpell.displayName()),
                true
        );
        return false;
    }
}
