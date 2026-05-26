package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class WandSpellExtractionEvents {
    private WandSpellExtractionEvents() {
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack paper = event.getItemStack();
        if (!paper.is(Items.PAPER) || paper.hasTag()) {
            return;
        }

        InteractionHand paperHand = event.getHand();
        InteractionHand wandHand = paperHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack wand = event.getEntity().getItemInHand(wandHand);
        if (!isWand(wand)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) {
            return;
        }

        extractActiveSpell(event.getEntity(), paperHand, paper, wand);
    }

    private static void extractActiveSpell(Player player, InteractionHand paperHand, ItemStack paper, ItemStack wand) {
        WandTemplates.ensureInitialized(wand, wand.is(ModItems.ADMIN_WAND.get()));
        WandData data = WandData.read(wand);
        if (data.spells().size() <= 1) {
            player.displayClientMessage(Component.literal("A wand must keep at least one spell.").withStyle(ChatFormatting.YELLOW), false);
            return;
        }

        int spellIndex = data.activeIndex();
        WandData.WandSpellData active = data.spells().get(spellIndex);
        Spell spell = SpellRegistry.get(active.key());
        if (spell == null) {
            player.displayClientMessage(Component.literal("That wand's active spell is invalid.").withStyle(ChatFormatting.RED), false);
            return;
        }

        ItemStack spellPaper = new ItemStack(Items.PAPER);
        new SpellInstance(active.key(), active.displayName(), active.manaCost(), active.cooldownTicks()).writeToItem(spellPaper);
        if (!data.removeSpell(spellIndex)) {
            player.displayClientMessage(Component.literal("Could not remove that spell.").withStyle(ChatFormatting.RED), false);
            return;
        }

        data.save(wand);
        if (paper.getCount() == 1) {
            player.setItemInHand(paperHand, spellPaper);
        } else {
            if (!player.getAbilities().instabuild) {
                paper.shrink(1);
            }
            if (!player.getInventory().add(spellPaper)) {
                player.drop(spellPaper, false);
            }
        }
        player.getInventory().setChanged();
        player.displayClientMessage(Component.literal("Removed " + active.displayName() + " from wand").withStyle(ChatFormatting.AQUA), false);
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }
}
