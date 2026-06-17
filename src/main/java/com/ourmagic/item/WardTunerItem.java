package com.ourmagic.item;

import com.ourmagic.magic.ward.WorldWards;
import com.ourmagic.registry.ModBlocks;
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

public class WardTunerItem extends Item {
    public static final String TAG_WARD_STONE = "OurMagicWardStone";
    public static final String TAG_DIMENSION = "OurMagicWardDimension";

    public WardTunerItem(Properties properties) {
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
        if (level.getBlockState(clicked).is(ModBlocks.WARD_STONE.get())) {
            rememberWardStone(context, serverLevel, clicked);
            player.displayClientMessage(Component.literal("Ward Tuner bound to Ward Stone at " + clicked.toShortString()).withStyle(ChatFormatting.AQUA), false);
            WorldWards.showOutline(serverLevel, clicked);
            return InteractionResult.CONSUME;
        }

        boolean linkedPerimeter = WorldWards.isLinkedPerimeter(serverLevel, clicked);
        if (!level.getBlockState(clicked).is(ModBlocks.WARD_PERIMETER_STONE.get()) && !linkedPerimeter) {
            player.displayClientMessage(Component.literal("Click a Ward Stone, then a Ward Perimeter Stone.").withStyle(ChatFormatting.YELLOW), false);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            if (WorldWards.unlinkPerimeter(serverLevel, clicked)) {
                player.displayClientMessage(Component.literal("Unlinked Ward Perimeter Stone at " + clicked.toShortString()).withStyle(ChatFormatting.YELLOW), false);
            } else {
                player.displayClientMessage(Component.literal("That Ward Perimeter Stone was not linked.").withStyle(ChatFormatting.GRAY), false);
            }
            return InteractionResult.CONSUME;
        }

        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        if (!tag.contains(TAG_WARD_STONE) || !tag.contains(TAG_DIMENSION)) {
            player.displayClientMessage(Component.literal("Click a Ward Stone first.").withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }
        ResourceLocation storedDimension = ResourceLocation.tryParse(tag.getString(TAG_DIMENSION));
        if (!serverLevel.dimension().location().equals(storedDimension)) {
            player.displayClientMessage(Component.literal("That Ward Stone is in another dimension.").withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        BlockPos wardStone = BlockPos.of(tag.getLong(TAG_WARD_STONE));
        WorldWards.ApplyResult result = WorldWards.linkPerimeter(serverLevel, wardStone, clicked);
        if (result.success()) {
            player.displayClientMessage(Component.literal(result.message()).withStyle(ChatFormatting.AQUA), false);
            WorldWards.showOutline(serverLevel, wardStone);
        } else {
            player.displayClientMessage(Component.literal(result.message()).withStyle(ChatFormatting.RED), false);
        }
        return InteractionResult.CONSUME;
    }

    private static void rememberWardStone(UseOnContext context, ServerLevel level, BlockPos pos) {
        CompoundTag tag = context.getItemInHand().getOrCreateTag();
        tag.putLong(TAG_WARD_STONE, pos.asLong());
        tag.putString(TAG_DIMENSION, level.dimension().location().toString());
    }

    public static boolean hasBoundWardStone(net.minecraft.world.item.ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains(TAG_WARD_STONE) && tag.contains(TAG_DIMENSION);
    }

    public static boolean isBoundToDimension(net.minecraft.world.item.ItemStack stack, ServerLevel level) {
        ResourceLocation storedDimension = ResourceLocation.tryParse(stack.getOrCreateTag().getString(TAG_DIMENSION));
        return level.dimension().location().equals(storedDimension);
    }

    public static BlockPos boundWardStone(net.minecraft.world.item.ItemStack stack) {
        return BlockPos.of(stack.getOrCreateTag().getLong(TAG_WARD_STONE));
    }
}
