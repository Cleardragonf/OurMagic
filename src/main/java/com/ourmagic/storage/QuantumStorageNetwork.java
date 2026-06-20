package com.ourmagic.storage;

import com.ourmagic.block.entity.CrystalDriveBayBlockEntity;
import com.ourmagic.item.CrystalDriveItem;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public final class QuantumStorageNetwork {
    public static final int RANGE = 8;

    private QuantumStorageNetwork() {
    }

    public static List<Entry> entries(ServerLevel level, BlockPos corePos) {
        List<Entry> entries = new ArrayList<>();
        forEachDrive(level, corePos, (bay, slot, drive, crystalDrive) -> {
            for (CrystalDriveItem.StoredEntry storedEntry : CrystalDriveItem.storedEntries(drive)) {
                ItemStack stored = storedEntry.stack();
                int count = storedEntry.count();
                if (stored.isEmpty() || count <= 0) {
                    continue;
                }
                boolean merged = false;
                for (int i = 0; i < entries.size(); i++) {
                    Entry entry = entries.get(i);
                    if (ItemStack.isSameItemSameTags(entry.stack(), stored)) {
                        entries.set(i, new Entry(entry.stack(), entry.count() + count, entry.capacity() + crystalDrive.capacity()));
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    ItemStack display = stored.copy();
                    display.setCount(1);
                    entries.add(new Entry(display, count, crystalDrive.capacity()));
                }
            }
        });
        entries.sort((a, b) -> a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString()));
        return List.copyOf(entries);
    }

    public static ItemStack extract(ServerLevel level, BlockPos corePos, int entryIndex, int amount) {
        List<Entry> current = entries(level, corePos);
        if (entryIndex < 0 || entryIndex >= current.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack target = current.get(entryIndex).stack();
        ItemStack result = ItemStack.EMPTY;
        int remaining = amount;
        for (CrystalDriveRef ref : drives(level, corePos)) {
            if (remaining <= 0) {
                break;
            }
            ItemStack stored = CrystalDriveItem.storedItem(ref.drive());
            ItemStack extracted = ref.crystalDrive().extractMatching(ref.drive(), target, remaining, false);
            if (extracted.isEmpty()) {
                continue;
            }
            ref.bay().setChanged();
            remaining -= extracted.getCount();
            if (result.isEmpty()) {
                result = extracted;
            } else {
                result.grow(extracted.getCount());
            }
        }
        return result;
    }

    public static ItemStack insert(ServerLevel level, BlockPos corePos, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        for (CrystalDriveRef ref : drives(level, corePos)) {
            if (remaining.isEmpty()) {
                break;
            }
            int accepted = ref.crystalDrive().insert(ref.drive(), remaining, false);
            if (accepted > 0) {
                remaining.shrink(accepted);
                ref.bay().setChanged();
            }
        }
        return remaining;
    }

    public static Summary summary(ServerLevel level, BlockPos corePos) {
        int bays = 0;
        int drives = 0;
        int used = 0;
        int capacity = 0;
        for (BlockPos pos : bayPositions(level, corePos)) {
            bays++;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CrystalDriveBayBlockEntity bay)) {
                continue;
            }
            IItemHandler slots = bay.driveSlots();
            for (int i = 0; i < slots.getSlots(); i++) {
                ItemStack drive = slots.getStackInSlot(i);
                if (drive.getItem() instanceof CrystalDriveItem crystalDrive) {
                    drives++;
                    used += CrystalDriveItem.storedCount(drive);
                    capacity += crystalDrive.capacity();
                }
            }
        }
        return new Summary(bays, drives, used, capacity);
    }

    private static List<CrystalDriveRef> drives(ServerLevel level, BlockPos corePos) {
        List<CrystalDriveRef> drives = new ArrayList<>();
        forEachDrive(level, corePos, (bay, slot, drive, crystalDrive) -> drives.add(new CrystalDriveRef(bay, slot, drive, crystalDrive)));
        return drives;
    }

    private static void forEachDrive(ServerLevel level, BlockPos corePos, DriveConsumer consumer) {
        for (BlockPos pos : bayPositions(level, corePos)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CrystalDriveBayBlockEntity bay)) {
                continue;
            }
            IItemHandler slots = bay.driveSlots();
            for (int slot = 0; slot < slots.getSlots(); slot++) {
                ItemStack drive = slots.getStackInSlot(slot);
                if (drive.getItem() instanceof CrystalDriveItem crystalDrive) {
                    consumer.accept(bay, slot, drive, crystalDrive);
                }
            }
        }
    }

    private static List<BlockPos> bayPositions(ServerLevel level, BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.betweenClosedStream(corePos.offset(-RANGE, -RANGE, -RANGE), corePos.offset(RANGE, RANGE, RANGE))
                .filter(pos -> level.getBlockState(pos).is(ModBlocks.CRYSTAL_DRIVE_BAY.get()))
                .forEach(pos -> positions.add(pos.immutable()));
        return positions;
    }

    private interface DriveConsumer {
        void accept(CrystalDriveBayBlockEntity bay, int slot, ItemStack drive, CrystalDriveItem crystalDrive);
    }

    public record Entry(ItemStack stack, int count, int capacity) {
    }

    public record Summary(int bays, int drives, int used, int capacity) {
    }

    private record CrystalDriveRef(CrystalDriveBayBlockEntity bay, int slot, ItemStack drive, CrystalDriveItem crystalDrive) {
    }
}
