package com.ourmagic.block;

import com.ourmagic.block.entity.ArcaneQuarryBlockEntity;
import com.ourmagic.registry.ModItems;
import com.ourmagic.util.PlayerTitles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import com.ourmagic.ui.ArcaneQuarryMenu;

import javax.annotation.Nullable;

public class ArcaneQuarryBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    public ArcaneQuarryBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneQuarryBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, tickerPos, blockState, blockEntity) -> {
            if (blockEntity instanceof ArcaneQuarryBlockEntity quarry) {
                ArcaneQuarryBlockEntity.serverTick(tickerLevel, tickerPos, blockState, quarry);
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
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ArcaneQuarryBlockEntity quarry) {
            if (serverPlayer.isShiftKeyDown()) {
                PlayerTitles.show(serverPlayer,
                        Component.literal("Arcane Quarry").withStyle(ChatFormatting.AQUA),
                        Component.literal(quarry.bindToNearestMarkers(serverPlayer)).withStyle(ChatFormatting.GRAY));
            } else {
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Arcane Quarry");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                        return new ArcaneQuarryMenu(containerId, inventory, pos);
                    }
                }, buffer -> buffer.writeBlockPos(pos));
            }
        }
        return InteractionResult.CONSUME;
    }
}
