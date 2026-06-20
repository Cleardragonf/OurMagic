package com.ourmagic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CrystalDriveItem extends Item {
    private static final String TAG_ITEMS = "StoredItems";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_ITEM = "StoredItem";
    private static final String TAG_COUNT = "StoredCount";
    private final int capacity;

    public CrystalDriveItem(Properties properties, int capacity) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public static ItemStack storedItem(ItemStack drive) {
        List<StoredEntry> entries = storedEntries(drive);
        return entries.isEmpty() ? ItemStack.EMPTY : entries.get(0).stack().copy();
    }

    public static int storedCount(ItemStack drive) {
        int total = 0;
        for (StoredEntry entry : storedEntries(drive)) {
            total += entry.count();
        }
        return total;
    }

    public static List<StoredEntry> storedEntries(ItemStack drive) {
        CompoundTag tag = drive.getOrCreateTag();
        List<StoredEntry> entries = new ArrayList<>();
        if (tag.contains(TAG_ITEMS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                ItemStack stack = ItemStack.of(entryTag.getCompound(TAG_STACK));
                int count = Math.max(0, entryTag.getInt(TAG_COUNT));
                if (!stack.isEmpty() && count > 0) {
                    stack.setCount(1);
                    entries.add(new StoredEntry(stack, count));
                }
            }
            return List.copyOf(entries);
        }
        if (tag.contains(TAG_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.of(tag.getCompound(TAG_ITEM));
            int count = Math.max(0, tag.getInt(TAG_COUNT));
            if (!stack.isEmpty() && count > 0) {
                stack.setCount(1);
                entries.add(new StoredEntry(stack, count));
            }
        }
        return List.copyOf(entries);
    }

    public int insert(ItemStack drive, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return 0;
        }
        List<StoredEntry> entries = new ArrayList<>(storedEntries(drive));
        int count = entries.stream().mapToInt(StoredEntry::count).sum();
        int accepted = Math.min(stack.getCount(), capacity - count);
        if (accepted > 0 && !simulate) {
            boolean merged = false;
            for (int i = 0; i < entries.size(); i++) {
                StoredEntry entry = entries.get(i);
                if (ItemStack.isSameItemSameTags(entry.stack(), stack)) {
                    entries.set(i, new StoredEntry(entry.stack(), entry.count() + accepted));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                ItemStack storedCopy = stack.copy();
                storedCopy.setCount(1);
                entries.add(new StoredEntry(storedCopy, accepted));
            }
            saveEntries(drive, entries);
        }
        return accepted;
    }

    public ItemStack extract(ItemStack drive, int amount, boolean simulate) {
        List<StoredEntry> entries = storedEntries(drive);
        if (entries.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        return extractMatching(drive, entries.get(0).stack(), amount, simulate);
    }

    public ItemStack extractMatching(ItemStack drive, ItemStack target, int amount, boolean simulate) {
        if (target.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        List<StoredEntry> entries = new ArrayList<>(storedEntries(drive));
        for (int i = 0; i < entries.size(); i++) {
            StoredEntry entry = entries.get(i);
            if (!ItemStack.isSameItemSameTags(entry.stack(), target)) {
                continue;
            }
            int count = entry.count();
            int extracted = Math.min(amount, count);
            ItemStack result = entry.stack().copy();
            result.setCount(extracted);
            if (!simulate) {
                int remaining = count - extracted;
                if (remaining <= 0) {
                    entries.remove(i);
                } else {
                    entries.set(i, new StoredEntry(entry.stack(), remaining));
                }
                saveEntries(drive, entries);
            }
            return result;
        }
        return ItemStack.EMPTY;
    }

    private static void saveEntries(ItemStack drive, List<StoredEntry> entries) {
        CompoundTag tag = drive.getOrCreateTag();
        tag.remove(TAG_ITEM);
        tag.remove(TAG_COUNT);
        ListTag list = new ListTag();
        for (StoredEntry entry : entries) {
            if (entry.stack().isEmpty() || entry.count() <= 0) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            ItemStack stack = entry.stack().copy();
            stack.setCount(1);
            entryTag.put(TAG_STACK, stack.save(new CompoundTag()));
            entryTag.putInt(TAG_COUNT, entry.count());
            list.add(entryTag);
        }
        tag.put(TAG_ITEMS, list);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = storedCount(stack);
        tooltip.add(Component.literal(count + "/" + capacity + " items").withStyle(ChatFormatting.AQUA));
        List<StoredEntry> entries = storedEntries(stack);
        if (!entries.isEmpty()) {
            tooltip.add(Component.literal(entries.size() + " item types").withStyle(ChatFormatting.GRAY));
        }
    }

    public record StoredEntry(ItemStack stack, int count) {
    }
}
