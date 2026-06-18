package com.ourmagic.block;

import com.ourmagic.block.entity.CreativeRfGeneratorBlockEntity;
import com.ourmagic.util.PlayerTitles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class CreativeRfGeneratorBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(1.0D, 0.0D, 2.0D, 15.0D, 8.0D, 14.0D),
            box(3.0D, 8.0D, 4.0D, 13.0D, 12.0D, 12.0D),
            box(5.0D, 12.0D, 5.0D, 11.0D, 15.0D, 11.0D),
            box(6.0D, 15.0D, 6.0D, 10.0D, 16.0D, 10.0D)
    ).optimize();

    public CreativeRfGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeRfGeneratorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof CreativeRfGeneratorBlockEntity generator) {
                CreativeRfGeneratorBlockEntity.serverTick(tickerLevel, pos, blockState, generator);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CreativeRfGeneratorBlockEntity generator) {
            PlayerTitles.show(serverPlayer,
                    Component.literal("Creative RF Generator").withStyle(ChatFormatting.GOLD),
                    Component.literal(generator.statusLine()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() > 0.35F) {
            return;
        }
        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.14D;
        double y = pos.getY() + 1.05D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.14D;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.035D + random.nextDouble() * 0.025D, 0.0D);
    }
}
