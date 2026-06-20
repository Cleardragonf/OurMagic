package com.ourmagic.block;

import com.ourmagic.block.entity.QuarryMarkerBlockEntity;
import com.ourmagic.util.PlayerTitles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class QuarryMarkerBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(5.0D, 0.0D, 5.0D, 11.0D, 14.0D, 11.0D);

    public QuarryMarkerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuarryMarkerBlockEntity(pos, state);
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof QuarryMarkerBlockEntity marker) {
            marker.setOwner(player);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof QuarryMarkerBlockEntity marker) {
            PlayerTitles.show(serverPlayer,
                    Component.literal("Quarry Marker").withStyle(ChatFormatting.AQUA),
                    Component.literal(marker.statusLine()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.CONSUME;
    }
}
