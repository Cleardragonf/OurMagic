package com.ourmagic.block.entity;

import com.ourmagic.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class WardCamouflageBlockEntity extends BlockEntity {
    private static final String TAG_MIMIC = "Mimic";
    private BlockState mimicState = Blocks.AIR.defaultBlockState();

    public WardCamouflageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARD_CAMOUFLAGE.get(), pos, state);
    }

    public BlockState mimicState() {
        return mimicState;
    }

    public void setMimicState(BlockState mimicState) {
        this.mimicState = mimicState;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_MIMIC, 10)) {
            mimicState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(TAG_MIMIC));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(TAG_MIMIC, NbtUtils.writeBlockState(mimicState));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
