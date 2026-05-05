package com.ourmagic.item;

import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.network.CraftSpellPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GrimoireItem extends Item {
    private static final String TAG_SPELLS = "OurMagicSpells";
    private static final String TAG_SELECTED = "OurMagicSelected";

    public GrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ItemStack otherHand = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            String paperSpell = spellPaperKey(otherHand);
            if (!paperSpell.isEmpty()) {
                if (addSpell(stack, paperSpell)) {
                    if (!player.getAbilities().instabuild) {
                        otherHand.shrink(1);
                    }
                    player.displayClientMessage(Component.literal("Added " + paperSpell + " to grimoire").withStyle(ChatFormatting.AQUA), false);
                } else {
                    player.displayClientMessage(Component.literal("That grimoire already contains " + paperSpell).withStyle(ChatFormatting.YELLOW), false);
                }
                return InteractionResultHolder.success(stack);
            }

            if (spellCount(stack) <= 0) {
                player.displayClientMessage(Component.literal("This grimoire has no spells.").withStyle(ChatFormatting.GRAY), false);
            } else {
                cycle(stack, player.isShiftKeyDown() ? -1 : 1);
                player.displayClientMessage(Component.literal("Selected " + selectedSpell(stack)).withStyle(ChatFormatting.AQUA), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = spellCount(stack);
        tooltip.add(Component.literal("Stored spells: " + count).withStyle(ChatFormatting.AQUA));
        if (count > 0) {
            tooltip.add(Component.literal("Selected: " + selectedSpell(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("Right click cycles spells").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Sneak + right click cycles backward").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean addSpell(ItemStack stack, String spellKey) {
        if (SpellRegistry.get(spellKey) == null || containsSpell(stack, spellKey)) {
            return false;
        }

        ListTag spells = spells(stack);
        spells.add(StringTag.valueOf(spellKey));
        stack.getOrCreateTag().put(TAG_SPELLS, spells);
        stack.getOrCreateTag().putString(CraftSpellPacket.TAG_SPELL_KEY, spellKey);
        stack.setHoverName(Component.literal("Grimoire").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    public static String selectedSpell(ItemStack stack) {
        ListTag spells = spells(stack);
        if (spells.isEmpty()) {
            return "";
        }

        int selected = Math.max(0, Math.min(stack.getOrCreateTag().getInt(TAG_SELECTED), spells.size() - 1));
        return spells.getString(selected);
    }

    public static boolean hasSelectedSpell(ItemStack stack) {
        return !selectedSpell(stack).isEmpty();
    }

    public static List<String> spellKeys(ItemStack stack) {
        ListTag spells = spells(stack);
        java.util.ArrayList<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < spells.size(); i++) {
            keys.add(spells.getString(i));
        }
        return List.copyOf(keys);
    }

    private static String spellPaperKey(ItemStack stack) {
        if (!stack.is(Items.PAPER) || !stack.hasTag()) {
            return "";
        }

        String key = stack.getOrCreateTag().getString(CraftSpellPacket.TAG_SPELL_KEY);
        return SpellRegistry.get(key) == null ? "" : key;
    }

    private static boolean containsSpell(ItemStack stack, String spellKey) {
        ListTag spells = spells(stack);
        for (int i = 0; i < spells.size(); i++) {
            if (spells.getString(i).equals(spellKey)) {
                return true;
            }
        }
        return false;
    }

    private static void cycle(ItemStack stack, int offset) {
        int count = spellCount(stack);
        if (count <= 0) {
            return;
        }

        int selected = Math.floorMod(stack.getOrCreateTag().getInt(TAG_SELECTED) + offset, count);
        stack.getOrCreateTag().putInt(TAG_SELECTED, selected);
        stack.getOrCreateTag().putString(CraftSpellPacket.TAG_SPELL_KEY, selectedSpell(stack));
    }

    private static int spellCount(ItemStack stack) {
        return spells(stack).size();
    }

    private static ListTag spells(ItemStack stack) {
        return stack.getOrCreateTag().getList(TAG_SPELLS, 8);
    }
}
