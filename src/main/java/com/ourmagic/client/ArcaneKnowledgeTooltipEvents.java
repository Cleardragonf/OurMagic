package com.ourmagic.client;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.ArcaneKnowledgeBook;
import com.ourmagic.magic.SpellIngredients;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.network.CraftSpellPacket;
import com.ourmagic.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ArcaneKnowledgeTooltipEvents {
    private ArcaneKnowledgeTooltipEvents() {
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(Items.PAPER) && stack.hasTag() && stack.getOrCreateTag().contains(CraftSpellPacket.TAG_SPELL_KEY)) {
            SpellInstance spell = SpellInstance.fromItem(stack);
            event.getToolTip().add(Component.literal("Recipe: " + spell.key()).withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(Component.literal("Mana: " + spell.manaCost()).withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.literal(String.format("Cooldown: %.1fs", spell.cooldownTicks() / 20.0F)).withStyle(ChatFormatting.GOLD));
            return;
        }

        if (!ArcaneKnowledgeBook.isKnowledgeBook(stack)) {
            return;
        }

        if (Minecraft.getInstance().screen instanceof MerchantScreen) {
            event.getToolTip().clear();
            event.getToolTip().add(Component.translatable(Items.BOOK.getDescriptionId()));
            return;
        }

        List<String> spells = ArcaneKnowledgeBook.spellKeys(stack);
        event.getToolTip().add(Component.literal("Spellcraft notes").withStyle(ChatFormatting.DARK_PURPLE));
        for (int i = 0; i < Math.min(4, spells.size()); i++) {
            String spell = spells.get(i);
            event.getToolTip().add(Component.literal("- " + spell).withStyle(ChatFormatting.LIGHT_PURPLE));
            event.getToolTip().add(Component.literal("  1x " + ModItems.WAND.get().getDescription().getString()).withStyle(ChatFormatting.GRAY));
            List<SpellIngredients.Requirement> requirements = SpellIngredients.requirementsFor(spell);
            for (int r = 0; r < Math.min(3, requirements.size()); r++) {
                SpellIngredients.Requirement requirement = requirements.get(r);
                event.getToolTip().add(Component.literal("  " + requirement.count() + "x " + requirement.displayName().getString()).withStyle(ChatFormatting.GRAY));
            }
            if (requirements.size() > 3) {
                event.getToolTip().add(Component.literal("  ...").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (spells.size() > 4) {
            event.getToolTip().add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
