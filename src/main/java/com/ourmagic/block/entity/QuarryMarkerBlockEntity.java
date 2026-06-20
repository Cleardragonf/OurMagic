package com.ourmagic.block.entity;

import com.ourmagic.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public class QuarryMarkerBlockEntity extends BlockEntity {
    private static final String TAG_OWNER = "Owner";
    private UUID owner;

    public QuarryMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY_MARKER.get(), pos, state);
    }

    public void setOwner(ServerPlayer player) {
        if (owner == null) {
            owner = player.getUUID();
            setChanged();
        }
    }

    public boolean ownedBy(ServerPlayer player) {
        return owner == null || owner.equals(player.getUUID());
    }

    public String statusLine() {
        return owner == null ? "Unclaimed marker." : "Claimed marker at " + worldPosition.toShortString() + ".";
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID(TAG_OWNER, owner);
        }
    }
}
