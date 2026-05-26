package com.ourmagic.advancement;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class SpellAdvancementEvents {
    private SpellAdvancementEvents() {
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        awardKnownSpells(player, event.getCrafting());
    }

    public static void awardKnownSpells(ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.WAND.get()) && !stack.is(ModItems.ADMIN_WAND.get())) {
            return;
        }
        for (WandData.WandSpellData spell : WandData.read(stack).spells()) {
            ModCriteriaTriggers.awardSpell(player, spell.key());
        }
    }
}
