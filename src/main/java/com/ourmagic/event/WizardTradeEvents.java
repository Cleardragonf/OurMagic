package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.ArcaneKnowledgeBook;
import com.ourmagic.registry.ModItems;
import com.ourmagic.registry.ModVillagers;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class WizardTradeEvents {
    private WizardTradeEvents() {
    }

    @SubscribeEvent
    public static void villagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.WIZARD.get()) {
            return;
        }

        trades(event, 1).add(new DiamondItemTrade(ModItems.SPELL_FOCUS.get(), 2, 1, 12, 2));
        trades(event, 1).add(new ArcaneKnowledgeTrade(3, 10, 4));
        trades(event, 2).add(new DiamondItemTrade(ModItems.ANCHOR_TALISMAN.get(), 4, 1, 8, 8));
        trades(event, 2).add(new DiamondItemTrade(ModItems.SEEKER_TALISMAN.get(), 4, 1, 8, 8));
        trades(event, 3).add(new WandTrade(8, 6, 12));
        trades(event, 3).add(new DiamondItemTrade(ModItems.GRIMOIRE.get(), 10, 1, 4, 14));
        trades(event, 4).add(new DiamondItemTrade(ModItems.LIGHTNING_TALISMAN.get(), 12, 1, 4, 18));
        trades(event, 4).add(new DiamondItemTrade(ModItems.STORMCALL_TALISMAN.get(), 14, 1, 4, 18));
        trades(event, 5).add(new DiamondItemTrade(ModItems.CONFLUX_TALISMAN.get(), 18, 1, 3, 25));
        trades(event, 5).add(new DiamondItemTrade(ModItems.SANCTUARY_TALISMAN.get(), 18, 1, 3, 25));
    }

    private static List<VillagerTrades.ItemListing> trades(VillagerTradesEvent event, int level) {
        return event.getTrades().computeIfAbsent(level, ignored -> new ArrayList<>());
    }

    private record DiamondItemTrade(Item item, int diamonds, int count, int maxUses, int xp) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(new ItemStack(Items.DIAMOND, diamonds), new ItemStack(item, count), maxUses, xp, 0.05F);
        }
    }

    private record WandTrade(int diamonds, int maxUses, int xp) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            ItemStack wand = WandTemplates.applyRandom(new ItemStack(ModItems.WAND.get()), random);
            return new MerchantOffer(new ItemStack(Items.DIAMOND, diamonds), wand, maxUses, xp, 0.05F);
        }
    }

    private record ArcaneKnowledgeTrade(int diamonds, int maxUses, int xp) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(new ItemStack(Items.DIAMOND, diamonds), ArcaneKnowledgeBook.create(random, true), maxUses, xp, 0.05F);
        }
    }
}
