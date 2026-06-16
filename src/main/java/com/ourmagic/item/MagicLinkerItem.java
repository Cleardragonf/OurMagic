package com.ourmagic.item;

import com.ourmagic.block.entity.MagicAccumulatorBlockEntity;
import com.ourmagic.magic.energy.MagicEnergyReceiver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MagicLinkerItem extends Item {
    private static final String TAG_SOURCE = "OurMagicLinkSource";
    private static final String TAG_DIMENSION = "OurMagicLinkDimension";

    public MagicLinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        BlockPos clicked = context.getClickedPos();
        BlockEntity clickedEntity = level.getBlockEntity(clicked);
        if (player.isShiftKeyDown()) {
            if (clickedEntity instanceof MagicAccumulatorBlockEntity accumulator) {
                accumulator.cycleTransferMode();
                player.displayClientMessage(Component.literal("Accumulator mode: " + accumulator.transferMode().displayName())
                        .withStyle(ChatFormatting.AQUA), false);
            } else {
                clearSource(context);
                player.displayClientMessage(Component.literal("Magic Linker source cleared.").withStyle(ChatFormatting.YELLOW), false);
            }
            return InteractionResult.CONSUME;
        }

        if (clickedEntity instanceof MagicAccumulatorBlockEntity source && !hasSource(context)) {
            rememberSource(context, serverLevel, clicked);
            player.displayClientMessage(Component.literal("Magic Linker source set to accumulator at " + clicked.toShortString() + ".")
                    .withStyle(ChatFormatting.AQUA), false);
            return InteractionResult.CONSUME;
        }

        if (!hasSource(context)) {
            player.displayClientMessage(Component.literal("Click an accumulator first, then a magic receiver.").withStyle(ChatFormatting.YELLOW), false);
            return InteractionResult.CONSUME;
        }

        ResourceLocation storedDimension = ResourceLocation.tryParse(context.getItemInHand().getOrCreateTag().getString(TAG_DIMENSION));
        if (!serverLevel.dimension().location().equals(storedDimension)) {
            player.displayClientMessage(Component.literal("That accumulator is in another dimension.").withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        BlockPos sourcePos = BlockPos.of(context.getItemInHand().getOrCreateTag().getLong(TAG_SOURCE));
        BlockEntity sourceEntity = level.getBlockEntity(sourcePos);
        if (!(sourceEntity instanceof MagicAccumulatorBlockEntity source)) {
            clearSource(context);
            player.displayClientMessage(Component.literal("Stored accumulator no longer exists. Source cleared.").withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }
        if (!(clickedEntity instanceof MagicEnergyReceiver)) {
            player.displayClientMessage(Component.literal("That block cannot receive magic energy.").withStyle(ChatFormatting.YELLOW), false);
            return InteractionResult.CONSUME;
        }

        MagicAccumulatorBlockEntity.LinkResult result = source.toggleLink(clicked);
        ChatFormatting color = result.success() ? ChatFormatting.AQUA : ChatFormatting.RED;
        player.displayClientMessage(Component.literal(result.message()).withStyle(color), false);
        return InteractionResult.CONSUME;
    }

    private static boolean hasSource(UseOnContext context) {
        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        return tag.contains(TAG_SOURCE) && tag.contains(TAG_DIMENSION);
    }

    private static void rememberSource(UseOnContext context, ServerLevel level, BlockPos pos) {
        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        tag.putLong(TAG_SOURCE, pos.asLong());
        tag.putString(TAG_DIMENSION, level.dimension().location().toString());
    }

    private static void clearSource(UseOnContext context) {
        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        tag.remove(TAG_SOURCE);
        tag.remove(TAG_DIMENSION);
    }
}
