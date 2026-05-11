package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class GuideBookEvents {
    private static final String GIVEN_TAG = "OurMagicGuideBookGiven";

    private GuideBookEvents() {
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getPersistentData().getBoolean(GIVEN_TAG)) {
            return;
        }

        ItemStack guide = new ItemStack(ModItems.GUIDE_BOOK.get());
        if (!player.getInventory().add(guide)) {
            player.drop(guide, false);
        }
        player.getPersistentData().putBoolean(GIVEN_TAG, true);
    }
}
