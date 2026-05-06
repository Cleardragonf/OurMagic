package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.ArcaneKnowledgeBook;
import com.ourmagic.magic.SpellIngredients;
import com.ourmagic.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class ArcaneKnowledgeEvents {
    private static final float GENERAL_DROP_CHANCE = 0.05F;
    private static final float MAGIC_USER_DROP_CHANCE = 0.50F;

    private ArcaneKnowledgeEvents() {
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide || event.getSource().getEntity() == null) {
            return;
        }

        boolean magicUser = isMagicUser(event.getEntity().getType());
        float chance = magicUser ? MAGIC_USER_DROP_CHANCE : GENERAL_DROP_CHANCE;
        if (event.getEntity().getRandom().nextFloat() > chance) {
            return;
        }

        ItemStack book = ArcaneKnowledgeBook.create(event.getEntity().getRandom(), magicUser);
        event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), book));
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!ArcaneKnowledgeBook.isKnowledgeBook(stack)) {
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

    private static boolean isMagicUser(EntityType<?> type) {
        return type == EntityType.WITCH
                || type == EntityType.EVOKER
                || type == EntityType.ILLUSIONER
                || type == EntityType.VEX;
    }
}
