package com.ourmagic.block;

import com.ourmagic.block.entity.MagicFlowConverterBlockEntity;
import com.ourmagic.registry.ModItems;
import com.ourmagic.util.PlayerTitles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MagicFlowConverterBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D),
            box(5.0D, 4.0D, 5.0D, 11.0D, 12.0D, 11.0D),
            box(0.0D, 4.0D, 6.0D, 4.0D, 10.0D, 10.0D),
            box(12.0D, 4.0D, 6.0D, 16.0D, 10.0D, 10.0D),
            box(6.0D, 12.0D, 6.0D, 10.0D, 15.0D, 10.0D)
    ).optimize();

    public MagicFlowConverterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicFlowConverterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.MAGIC_LINKER.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MagicFlowConverterBlockEntity converter) {
            PlayerTitles.show(serverPlayer,
                    Component.literal("Magic Flow Converter").withStyle(ChatFormatting.AQUA),
                    Component.literal("Direct RF or Arcane ME -> MF. " + converter.linkedWardCoreSummary()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.CONSUME;
    }
}
