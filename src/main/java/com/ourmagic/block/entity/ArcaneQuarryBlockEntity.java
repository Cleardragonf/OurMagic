package com.ourmagic.block.entity;

import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.registry.ModBlockEntities;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ArcaneQuarryBlockEntity extends BlockEntity implements MagicEnergyReceiver {
    private static final String TAG_ENERGY = "Energy";
    private static final String TAG_MIN = "Min";
    private static final String TAG_MAX = "Max";
    private static final String TAG_CURSOR = "Cursor";
    private static final String TAG_PAUSED = "Paused";
    private static final String TAG_UPGRADES = "Upgrades";
    private static final int CAPACITY = 100_000;
    private static final int COST_PER_BLOCK = 80;
    private static final int TICKS_PER_BLOCK = 12;
    private static final int MAX_MARKER_RANGE = 64;
    private static final int MAX_LINKS = 8;
    private int energy;
    private BlockPos min;
    private BlockPos max;
    private BlockPos cursor;
    private boolean paused;
    private final ItemStackHandler upgrades = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            setChangedAndUpdate();
        }
    };

    public ArcaneQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_QUARRY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneQuarryBlockEntity quarry) {
        if (!(level instanceof ServerLevel serverLevel) || quarry.paused || quarry.min == null || quarry.max == null || level.getGameTime() % Math.max(2, TICKS_PER_BLOCK - quarry.upgradeCount(Items.SUGAR) * 2) != 0L) {
            return;
        }
        quarry.mineNext(serverLevel);
    }

    @Override
    public int receiveMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, CAPACITY - energy);
        if (accepted > 0 && !simulate) {
            energy += accepted;
            setChangedAndUpdate();
        }
        return accepted;
    }

    public int storedEnergy() {
        return energy;
    }

    public int capacity() {
        return CAPACITY;
    }

    public int inboundLinkCount() {
        return level instanceof ServerLevel serverLevel ? MagicLinkNetwork.inboundLinkCount(serverLevel, worldPosition) : 0;
    }

    public int maxLinks() {
        return MAX_LINKS;
    }

    public int totalLinkCount(ServerLevel level) {
        return MagicLinkNetwork.inboundLinkCount(level, worldPosition);
    }

    public String bindToNearestMarkers(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return "Quarry is not ready.";
        }
        List<BlockPos> markers = ownedMarkers(serverLevel, player);
        if (markers.size() < 2) {
            return "Place two owned Quarry Markers within " + MAX_MARKER_RANGE + " blocks, then shift-right-click the quarry.";
        }
        BlockPos a = markers.get(0);
        BlockPos b = markers.get(1);
        min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        cursor = min.immutable();
        setChanged();
        return "Bound area " + min.toShortString() + " -> " + max.toShortString() + ".";
    }

    public String statusLine() {
        String area = min == null || max == null ? "Unbound" : min.toShortString() + " -> " + max.toShortString();
        return (paused ? "Paused, " : "Running, ") + area + ", " + energy + "/" + CAPACITY + " MF, cost " + effectiveCostPerBlock() + " MF/block.";
    }

    public boolean paused() {
        return paused;
    }

    public void togglePaused() {
        paused = !paused;
        setChangedAndUpdate();
    }

    public void resetArea() {
        min = null;
        max = null;
        cursor = null;
        setChangedAndUpdate();
    }

    public IItemHandler upgrades() {
        return upgrades;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = Math.max(0, tag.getInt(TAG_ENERGY));
        min = tag.contains(TAG_MIN) ? BlockPos.of(tag.getLong(TAG_MIN)) : null;
        max = tag.contains(TAG_MAX) ? BlockPos.of(tag.getLong(TAG_MAX)) : null;
        cursor = tag.contains(TAG_CURSOR) ? BlockPos.of(tag.getLong(TAG_CURSOR)) : null;
        paused = tag.getBoolean(TAG_PAUSED);
        upgrades.deserializeNBT(tag.getCompound(TAG_UPGRADES));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_ENERGY, energy);
        if (min != null) {
            tag.putLong(TAG_MIN, min.asLong());
        }
        if (max != null) {
            tag.putLong(TAG_MAX, max.asLong());
        }
        if (cursor != null) {
            tag.putLong(TAG_CURSOR, cursor.asLong());
        }
        tag.putBoolean(TAG_PAUSED, paused);
        tag.put(TAG_UPGRADES, upgrades.serializeNBT());
    }

    private void mineNext(ServerLevel level) {
        int cost = effectiveCostPerBlock();
        if (energy < cost) {
            return;
        }
        if (cursor == null || !inside(cursor)) {
            cursor = min.immutable();
        }
        int attempts = Math.max(1, volume());
        for (int i = 0; i < attempts; i++) {
            BlockPos target = cursor.immutable();
            cursor = next(cursor);
            if (target.equals(worldPosition) || level.getBlockState(target).is(ModBlocks.ARCANE_QUARRY.get()) || level.getBlockState(target).is(ModBlocks.QUARRY_MARKER.get())) {
                continue;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.isAir() || targetState.getDestroySpeed(level, target) < 0.0F || targetState.is(Blocks.BEDROCK)) {
                continue;
            }
            List<ItemStack> drops = Block.getDrops(targetState, level, target, level.getBlockEntity(target));
            if (!level.destroyBlock(target, false)) {
                continue;
            }
            energy -= cost;
            for (ItemStack drop : drops) {
                ItemStack remainder = insertAdjacent(drop);
                if (!remainder.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D, remainder);
                }
            }
            setChangedAndUpdate();
            return;
        }
    }

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private ItemStack insertAdjacent(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) {
                continue;
            }
            Optional<IItemHandler> handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve();
            if (handler.isEmpty()) {
                continue;
            }
            for (int slot = 0; slot < handler.get().getSlots() && !remaining.isEmpty(); slot++) {
                remaining = handler.get().insertItem(slot, remaining, false);
            }
        }
        return remaining;
    }

    private List<BlockPos> ownedMarkers(ServerLevel level, ServerPlayer player) {
        List<BlockPos> markers = new ArrayList<>();
        BlockPos.betweenClosedStream(worldPosition.offset(-MAX_MARKER_RANGE, -MAX_MARKER_RANGE, -MAX_MARKER_RANGE), worldPosition.offset(MAX_MARKER_RANGE, MAX_MARKER_RANGE, MAX_MARKER_RANGE))
                .filter(pos -> level.getBlockState(pos).is(ModBlocks.QUARRY_MARKER.get()))
                .forEach(pos -> {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof QuarryMarkerBlockEntity marker && marker.ownedBy(player)) {
                        markers.add(pos.immutable());
                    }
                });
        markers.sort(Comparator.comparingDouble(pos -> pos.distSqr(worldPosition)));
        return markers;
    }

    private boolean inside(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private BlockPos next(BlockPos pos) {
        int x = pos.getX() + 1;
        int y = pos.getY();
        int z = pos.getZ();
        if (x > max.getX()) {
            x = min.getX();
            z++;
        }
        if (z > max.getZ()) {
            z = min.getZ();
            y++;
        }
        if (y > max.getY()) {
            y = min.getY();
        }
        return new BlockPos(x, y, z);
    }

    private int volume() {
        return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
    }

    private int effectiveCostPerBlock() {
        return Math.max(10, COST_PER_BLOCK - upgradeCount(Items.REDSTONE) * 8);
    }

    private int upgradeCount(net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
