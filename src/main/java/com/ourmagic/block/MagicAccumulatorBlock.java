package com.ourmagic.block;

import com.ourmagic.block.entity.MagicAccumulatorBlockEntity;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.registry.ModItems;
import com.ourmagic.util.PlayerTitles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MagicAccumulatorBlock extends BaseEntityBlock {
    private final MagicEnergyType magicType;

    public MagicAccumulatorBlock(MagicEnergyType magicType, Properties properties) {
        super(properties);
        this.magicType = magicType;
    }

    public MagicEnergyType magicType() {
        return magicType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicAccumulatorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof MagicAccumulatorBlockEntity accumulator) {
                MagicAccumulatorBlockEntity.serverTick(tickerLevel, pos, blockState, accumulator);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.MAGIC_LINKER.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MagicAccumulatorBlockEntity accumulator) {
            PlayerTitles.show(serverPlayer,
                    Component.literal(magicType.displayName() + " Accumulator").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(accumulator.statusLine()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.CONSUME;
    }
}
