package com.ourmagic.item;

import com.ourmagic.magic.SpellInstance;
import com.ourmagic.network.CraftSpellPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WardDiagramItem extends Item {
    public WardDiagramItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(SpellInstance spell) {
        ItemStack stack = new ItemStack(com.ourmagic.registry.ModItems.WARD_DIAGRAM.get());
        spell.writeToItem(stack);
        stack.setHoverName(Component.literal("Ward Diagram: " + spell.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return stack;
    }

    public static boolean hasWard(ItemStack stack) {
        return stack.is(com.ourmagic.registry.ModItems.WARD_DIAGRAM.get())
                && stack.hasTag()
                && stack.getOrCreateTag().contains(CraftSpellPacket.TAG_SPELL_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasWard(stack)) {
            SpellInstance spell = SpellInstance.fromItem(stack);
            tooltip.add(Component.literal("Cast this onto a Ward Stone.").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Consumed when the ward is applied.").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal(spell.key()).withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            tooltip.add(Component.literal("A prepared ward belongs on a Ward Stone.").withStyle(ChatFormatting.GRAY));
        }
    }
}
