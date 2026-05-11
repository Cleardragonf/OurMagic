package com.ourmagic.item;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.mana.PlayerMana;
import com.ourmagic.network.CraftSpellPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import com.ourmagic.wand.WandModifier;
import com.ourmagic.wand.WandTemplates;
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

public class WandItem extends Item {
    private static final int SPELL_TOOLTIP_WINDOW = 8;
    private final boolean admin;

    public WandItem(Properties properties) {
        this(properties, false);
    }

    public WandItem(Properties properties, boolean admin) {
        super(properties);
        this.admin = admin;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WandTemplates.ensureInitialized(stack, admin);
        WandData data = WandData.read(stack);

        if (tryAddSpellSource(level, player, hand, stack, data)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                data.cycleSpell(1);
                data.save(stack);
                player.displayClientMessage(Component.translatable("message.ourmagic.cycled", data.activeSpellName()).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        Spell spell = SpellRegistry.get(data.activeSpell());
        if (spell == null) {
            player.displayClientMessage(Component.translatable("message.ourmagic.unsupported_spell", data.activeSpell()).withStyle(ChatFormatting.RED), false);
            return InteractionResultHolder.fail(stack);
        }

        if (data.cooldownUntil() > level.getGameTime()) {
            return InteractionResultHolder.fail(stack);
        }

        return castActiveSpell((ServerPlayer) player, stack, 1.0F)
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.fail(stack);
    }

    public static boolean castActiveSpell(ServerPlayer player, ItemStack stack, float chantMultiplier) {
        Level level = player.level();
        if (MagicStatusEffects.isSilenced(player)) {
            player.displayClientMessage(Component.literal("Your magic is silenced.").withStyle(ChatFormatting.DARK_PURPLE), true);
            return false;
        }

        WandTemplates.ensureInitialized(stack, stack.is(ModItems.ADMIN_WAND.get()));
        WandData data = WandData.read(stack);
        Spell spell = SpellRegistry.get(data.activeSpell());
        if (spell == null) {
            player.displayClientMessage(Component.translatable("message.ourmagic.unsupported_spell", data.activeSpell()).withStyle(ChatFormatting.RED), false);
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

        boolean cast = spell.cast(level, player, stack, data, chantMultiplier);
        if (cast) {
            if (!player.getAbilities().instabuild) {
                mana.spend(manaCost);
                ModNetwork.syncMana(player, mana);
            }
            int xp = Math.max(1, Math.round(5 * data.xpMultiplier()));
            int levelsGained = data.addActiveSpellXp(xp);
            int magicLevelsGained = mana.addMagicXp(Math.max(1, Math.round(xp * 0.10F)));
            if (levelsGained > 0) {
                celebrateSpellLevelUp(player, data.activeSpellName());
            }
            if (magicLevelsGained > 0) {
                player.displayClientMessage(Component.literal("Your magic reached level " + mana.magicLevel() + ".").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            int wandLevelsGained = data.addWandXp(xp);
            if (wandLevelsGained > 0) {
                player.displayClientMessage(Component.literal("Your wand reached level " + data.wandLevel() + ".").withStyle(ChatFormatting.GOLD), true);
            }
            ModNetwork.syncMana(player, mana);
            data.setCooldownUntil(level.getGameTime() + cooldownTicks);
            data.save(stack);
            player.getInventory().setChanged();
        }

        return cast;
    }

    private static boolean tryAddSpellSource(Level level, Player player, InteractionHand wandHand, ItemStack wand, WandData data) {
        InteractionHand sourceHand = wandHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack source = player.getItemInHand(sourceHand);
        boolean grimoire = source.is(com.ourmagic.registry.ModItems.GRIMOIRE.get()) && GrimoireItem.hasSelectedSpell(source);
        if (!grimoire && !isSpellPaper(source)) {
            return false;
        }

        if (level.isClientSide) {
            return true;
        }

        SpellInstance sourceSpell = grimoire ? GrimoireItem.selectedSpellInstance(source) : SpellInstance.fromItem(source);
        if (sourceSpell == null) {
            return true;
        }

        String spellKey = sourceSpell.key();
        Spell spell = SpellRegistry.get(spellKey);
        if (spell == null) {
            player.displayClientMessage(Component.literal("That spell source does not contain a valid spell.").withStyle(ChatFormatting.RED), false);
            return true;
        }

        if (!data.addSpell(sourceSpell)) {
            player.displayClientMessage(Component.literal("That wand already knows " + sourceSpell.displayName()).withStyle(ChatFormatting.YELLOW), false);
            return true;
        }

        data.save(wand);
        if (!grimoire && !player.getAbilities().instabuild) {
            source.shrink(1);
        }
        player.getInventory().setChanged();
        player.displayClientMessage(Component.literal("Added " + sourceSpell.displayName() + " to wand").withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    private static boolean isSpellPaper(ItemStack stack) {
        return stack.is(Items.PAPER)
                && stack.hasTag()
                && stack.getOrCreateTag().contains(CraftSpellPacket.TAG_SPELL_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        WandTemplates.ensureInitialized(stack, admin);
        WandData data = WandData.read(stack);
        int spellCount = data.spells().size();
        tooltip.add(Component.literal(data.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Wand Level: " + data.wandLevel() + " XP " + data.wandXp() + "/" + data.wandXpToNextLevel()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Modifiers: " + data.usedWandModifierSlots() + "/" + data.wandModifierSlots()).withStyle(ChatFormatting.GRAY));
        if (!data.modifiers().isEmpty()) {
            data.modifiers().forEach((key, levelValue) -> {
                String name = WandModifier.byKey(key).map(WandModifier::displayName).orElse(key);
                tooltip.add(Component.literal("  " + name + " " + levelValue).withStyle(ChatFormatting.DARK_AQUA));
            });
        }

        if (spellCount > 1) {
            tooltip.add(Component.literal("Number of spells: " + spellCount).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Active: " + data.activeSpellName()).withStyle(ChatFormatting.DARK_AQUA));
            if (hasShiftDown()) {
                appendNbtTooltip(data, tooltip);
            } else {
                tooltip.add(Component.literal("Hold Shift to inspect wand NBT").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.literal("Spell: " + data.activeSpellName()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Cost: " + data.activeManaCost() + " mana").withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.literal(String.format("Cooldown: %.1fs", data.activeCooldownTicks() / 20.0F)).withStyle(ChatFormatting.GOLD));
            if (hasShiftDown()) {
                appendNbtTooltip(data, tooltip);
            }
        }

        tooltip.add(Component.literal("Mana: Uses player pool").withStyle(ChatFormatting.BLUE));
        if (level != null && data.cooldownUntil() > level.getGameTime()) {
            float seconds = (data.cooldownUntil() - level.getGameTime()) / 20.0F;
            tooltip.add(Component.literal(String.format("Cooldown: %.1fs", seconds)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.literal("Cooldown: Ready").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.literal("Sneak + right click cycles spells").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        WandTemplates.ensureInitialized(stack, admin);
        return Component.literal(WandData.read(stack).displayName());
    }

    private static void appendNbtTooltip(WandData data, List<Component> tooltip) {
        tooltip.add(Component.literal("NBT").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  template: " + data.template()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(String.format("  power: %.2f", data.power())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("  active_index: " + data.activeIndex()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("  spells:").withStyle(ChatFormatting.DARK_GRAY));
        int spellCount = data.spells().size();
        int maxOffset = Math.max(0, spellCount - SPELL_TOOLTIP_WINDOW);
        int offset = Math.min(tooltipScrollOffset(), maxOffset);
        int end = Math.min(spellCount, offset + SPELL_TOOLTIP_WINDOW);
        for (int i = offset; i < end; i++) {
            WandData.WandSpellData spell = data.spells().get(i);
            ChatFormatting color = i == data.activeIndex() ? ChatFormatting.AQUA : ChatFormatting.GRAY;
            tooltip.add(Component.literal("    [" + i + "] " + spell.key() + " lvl=" + spell.level() + " xp=" + spell.xp() + "/" + spell.xpToNextLevel() + " pts=" + spell.attributePoints() + " cost=" + spell.manaCost() + " cooldown=" + spell.cooldownTicks() + " upgrades=c" + spell.chaining() + "/m" + spell.multistrike() + "/range" + spell.range() + "/radius" + spell.radius() + "/d" + spell.duration()).withStyle(color));
        }
        if (spellCount > SPELL_TOOLTIP_WINDOW) {
            tooltip.add(Component.literal("  showing " + (offset + 1) + "-" + end + " of " + spellCount + " | Shift + mouse wheel").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static boolean hasShiftDown() {
        Boolean shiftDown = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> com.ourmagic.client.ClientTooltipInput.hasShiftDown());
        return Boolean.TRUE.equals(shiftDown);
    }

    private static int tooltipScrollOffset() {
        Integer offset = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> com.ourmagic.client.ClientTooltipInput.scrollOffset());
        return offset == null ? 0 : Math.max(0, offset);
    }

    private static void celebrateSpellLevelUp(ServerPlayer player, String spellName) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.2D, player.getZ(), 45, 0.55D, 0.75D, 0.55D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(), 35, 0.6D, 0.7D, 0.6D, 0.04D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.7F, 1.25F);
        }

        Component message = Component.literal("Your " + spellName + " has leveled up!").withStyle(ChatFormatting.GOLD);
        player.connection.send(new ClientboundSetTitleTextPacket(message));
        player.displayClientMessage(message, true);
    }
}
