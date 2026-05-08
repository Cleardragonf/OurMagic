package com.ourmagic.item;

import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.network.CraftSpellPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
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
    private static final String TAG_MAGIC_CHARGE = "OurMagicCharge";
    private static final int MAX_MAGIC_CHARGE = 1000;
    private static final int SPELLCRAFT_CHARGE_COST = 25;

    public GrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ItemStack otherHand = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            SpellInstance paperSpell = spellPaper(otherHand);
            if (paperSpell != null) {
                if (addSpell(stack, paperSpell)) {
                    if (!player.getAbilities().instabuild) {
                        otherHand.shrink(1);
                    }
                    player.displayClientMessage(Component.literal("Added " + paperSpell.displayName() + " to grimoire").withStyle(ChatFormatting.AQUA), false);
                } else {
                    player.displayClientMessage(Component.literal("That grimoire already contains " + paperSpell.displayName()).withStyle(ChatFormatting.YELLOW), false);
                }
                return InteractionResultHolder.success(stack);
            }

            if (spellCount(stack) <= 0) {
                player.displayClientMessage(Component.literal("This grimoire has no spells.").withStyle(ChatFormatting.GRAY), false);
            } else {
                cycle(stack, player.isShiftKeyDown() ? -1 : 1);
                player.displayClientMessage(Component.literal("Selected " + selectedSpellDisplayName(stack)).withStyle(ChatFormatting.AQUA), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = spellCount(stack);
        tooltip.add(Component.literal("Stored spells: " + count).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Magic: " + magicCharge(stack) + "/" + MAX_MAGIC_CHARGE).withStyle(magicCharge(stack) > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
        if (count > 0) {
            SpellInstance selected = selectedSpellInstance(stack);
            tooltip.add(Component.literal("Selected: " + selectedSpellDisplayName(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (selected != null) {
                tooltip.add(Component.literal("Mana: " + selected.manaCost()).withStyle(ChatFormatting.BLUE));
                tooltip.add(Component.literal(String.format("Cooldown: %.1fs", selected.cooldownTicks() / 20.0F)).withStyle(ChatFormatting.GOLD));
            }
            tooltip.add(Component.literal("Right click cycles spells").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Sneak + right click cycles backward").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean addSpell(ItemStack stack, String spellKey) {
        return addSpell(stack, SpellInstance.fixed(spellKey));
    }

    public static boolean addSpell(ItemStack stack, SpellInstance spell) {
        String spellKey = spell.key();
        if (SpellRegistry.get(spellKey) == null || containsSpell(stack, spellKey)) {
            return false;
        }

        ListTag spells = spells(stack);
        spells.add(spell.write());
        stack.getOrCreateTag().put(TAG_SPELLS, spells);
        stack.getOrCreateTag().putString(CraftSpellPacket.TAG_SPELL_KEY, spellKey);
        spell.writeTo(stack.getOrCreateTag());
        stack.setHoverName(Component.literal("Grimoire").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    public static String selectedSpell(ItemStack stack) {
        ListTag spells = spells(stack);
        if (spells.isEmpty()) {
            return "";
        }

        int selected = Math.max(0, Math.min(stack.getOrCreateTag().getInt(TAG_SELECTED), spells.size() - 1));
        return spellAt(spells, selected).key();
    }

    public static SpellInstance selectedSpellInstance(ItemStack stack) {
        ListTag spells = spells(stack);
        if (spells.isEmpty()) {
            return null;
        }

        int selected = Math.max(0, Math.min(stack.getOrCreateTag().getInt(TAG_SELECTED), spells.size() - 1));
        return spellAt(spells, selected);
    }

    public static String selectedSpellDisplayName(ItemStack stack) {
        SpellInstance spell = selectedSpellInstance(stack);
        return spell == null ? "" : spell.displayName();
    }

    public static boolean hasSelectedSpell(ItemStack stack) {
        return !selectedSpell(stack).isEmpty();
    }

    public static List<String> spellKeys(ItemStack stack) {
        ListTag spells = spells(stack);
        java.util.ArrayList<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < spells.size(); i++) {
            keys.add(spellAt(spells, i).key());
        }
        return List.copyOf(keys);
    }

    public static boolean containsSpell(ItemStack stack, String spellKey) {
        ListTag spells = spells(stack);
        for (int i = 0; i < spells.size(); i++) {
            if (spellAt(spells, i).key().equals(spellKey)) {
                return true;
            }
        }
        return false;
    }

    public static int magicCharge(ItemStack stack) {
        if (!stack.hasTag() || !stack.getOrCreateTag().contains(TAG_MAGIC_CHARGE)) {
            return MAX_MAGIC_CHARGE;
        }
        return Math.max(0, Math.min(MAX_MAGIC_CHARGE, stack.getOrCreateTag().getInt(TAG_MAGIC_CHARGE)));
    }

    public static boolean hasMagicCharge(ItemStack stack) {
        return magicCharge(stack) > 0;
    }

    public static boolean consumeMagicCharge(ItemStack stack) {
        int charge = magicCharge(stack);
        if (charge <= 0) {
            return false;
        }
        stack.getOrCreateTag().putInt(TAG_MAGIC_CHARGE, Math.max(0, charge - SPELLCRAFT_CHARGE_COST));
        return true;
    }

    public static boolean addMagicCharge(ItemStack stack, int amount) {
        int charge = magicCharge(stack);
        if (charge >= MAX_MAGIC_CHARGE || amount <= 0) {
            return false;
        }
        stack.getOrCreateTag().putInt(TAG_MAGIC_CHARGE, Math.min(MAX_MAGIC_CHARGE, charge + amount));
        return true;
    }

    private static SpellInstance spellPaper(ItemStack stack) {
        if (!stack.is(Items.PAPER) || !stack.hasTag()) {
            return null;
        }

        String key = stack.getOrCreateTag().getString(CraftSpellPacket.TAG_SPELL_KEY);
        return SpellRegistry.get(key) == null ? null : SpellInstance.fromItem(stack);
    }

    private static void cycle(ItemStack stack, int offset) {
        int count = spellCount(stack);
        if (count <= 0) {
            return;
        }

        int selected = Math.floorMod(stack.getOrCreateTag().getInt(TAG_SELECTED) + offset, count);
        stack.getOrCreateTag().putInt(TAG_SELECTED, selected);
        SpellInstance selectedSpell = selectedSpellInstance(stack);
        if (selectedSpell != null) {
            selectedSpell.writeTo(stack.getOrCreateTag());
        }
    }

    private static int spellCount(ItemStack stack) {
        return spells(stack).size();
    }

    private static ListTag spells(ItemStack stack) {
        ListTag compound = stack.getOrCreateTag().getList(TAG_SPELLS, 10);
        if (!compound.isEmpty()) {
            return compound;
        }

        ListTag legacy = stack.getOrCreateTag().getList(TAG_SPELLS, 8);
        if (legacy.isEmpty()) {
            return compound;
        }

        ListTag migrated = new ListTag();
        for (int i = 0; i < legacy.size(); i++) {
            migrated.add(SpellInstance.fixed(legacy.getString(i)).write());
        }
        stack.getOrCreateTag().put(TAG_SPELLS, migrated);
        return migrated;
    }

    private static SpellInstance spellAt(ListTag spells, int index) {
        if (spells.get(index) instanceof CompoundTag tag) {
            return SpellInstance.fromTag(tag);
        }
        return SpellInstance.fixed(spells.getString(index));
    }
}
