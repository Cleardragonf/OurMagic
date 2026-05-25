package com.ourmagic.block;

import com.ourmagic.block.entity.WardStoneBlockEntity;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.magic.ward.WorldWards;
import com.ourmagic.network.CraftSpellPacket;
import com.ourmagic.util.PlayerTitles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class WardStoneBlock extends BaseEntityBlock {
    public WardStoneBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WardStoneBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return stack.is(Items.PAPER) && stack.hasTag() && stack.getOrCreateTag().contains(CraftSpellPacket.TAG_SPELL_KEY)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!stack.is(Items.PAPER) || !stack.hasTag() || !stack.getOrCreateTag().contains(CraftSpellPacket.TAG_SPELL_KEY)) {
            if (WorldWards.showOutline(serverLevel, pos)) {
                String message = WorldWards.describe(serverLevel, pos)
                        .orElseGet(() -> WardStoneBlockEntity.getOrCreate(serverLevel, pos)
                                .map(wardStone -> "MF " + wardStone.magicFlow() + "/" + wardStone.magicFlowCapacity()
                                        + ", " + wardStone.multiblockSize() + " stones, 0/" + wardStone.wardSlots()
                                        + " wards. Showing anchor-defined ward volume.")
                                .orElse("Showing anchor-defined ward volume."));
                PlayerTitles.show(serverPlayer,
                        Component.literal("Ward Outline").withStyle(ChatFormatting.AQUA),
                        Component.literal(message).withStyle(ChatFormatting.GRAY));
            } else {
                String charge = WardStoneBlockEntity.getOrCreate(serverLevel, pos)
                        .map(wardStone -> "MF " + wardStone.magicFlow() + "/" + wardStone.magicFlowCapacity() + ". ")
                        .orElse("");
                PlayerTitles.show(serverPlayer,
                        Component.literal("No Active Ward").withStyle(ChatFormatting.YELLOW),
                        Component.literal(charge + "This Ward Stone has no active ward.").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.CONSUME;
        }

        String spellKey = stack.getOrCreateTag().getString(CraftSpellPacket.TAG_SPELL_KEY);
        Spell spell = SpellRegistry.get(spellKey);
        if (spell == null || !SpellRegistry.payloadParts(spellKey).contains("ward")) {
            PlayerTitles.show(serverPlayer,
                    Component.literal("Invalid Ward Spell").withStyle(ChatFormatting.RED),
                    Component.literal("That paper does not hold a ward spell.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.CONSUME;
        }

        WorldWards.ApplyResult result = WorldWards.apply(serverLevel, pos, serverPlayer, SpellInstance.fromItem(stack));
        if (result.success()) {
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
            PlayerTitles.show(serverPlayer,
                    Component.literal("Ward Applied").withStyle(ChatFormatting.AQUA),
                    Component.literal(result.message()).withStyle(ChatFormatting.GRAY));
        } else {
            PlayerTitles.show(serverPlayer,
                    Component.literal("Ward Failed").withStyle(ChatFormatting.RED),
                    Component.literal(result.message()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            WorldWards.clear(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
