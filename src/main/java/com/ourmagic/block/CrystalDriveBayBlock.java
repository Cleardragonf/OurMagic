package com.ourmagic.block;

import com.ourmagic.block.entity.CrystalDriveBayBlockEntity;
import com.ourmagic.item.CrystalDriveItem;
import com.ourmagic.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Containers;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import com.ourmagic.ui.CrystalDriveBayMenu;

import javax.annotation.Nullable;

public class CrystalDriveBayBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);

    public CrystalDriveBayBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystalDriveBayBlockEntity(pos, state);
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.MAGIC_LINKER.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CrystalDriveBayBlockEntity bay) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof CrystalDriveItem) {
                if (bay.addDrive(held)) {
                    serverPlayer.displayClientMessage(Component.literal("Crystal drive installed.").withStyle(ChatFormatting.AQUA), true);
                } else {
                    serverPlayer.displayClientMessage(Component.literal("Drive bay is full.").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.CONSUME;
            }
            if (serverPlayer.isShiftKeyDown() && held.isEmpty()) {
                ItemStack removed = bay.removeDrive();
                if (!removed.isEmpty()) {
                    if (!serverPlayer.getInventory().add(removed)) {
                        serverPlayer.drop(removed, false);
                    }
                    serverPlayer.displayClientMessage(Component.literal("Crystal drive removed.").withStyle(ChatFormatting.YELLOW), true);
                } else {
                    serverPlayer.displayClientMessage(Component.literal("No crystal drives installed.").withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.CONSUME;
            }
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Crystal Drive Bay");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new CrystalDriveBayMenu(containerId, inventory, pos);
                }
            }, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrystalDriveBayBlockEntity bay) {
            ItemStack drive;
            while (!(drive = bay.removeDrive()).isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drive);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
