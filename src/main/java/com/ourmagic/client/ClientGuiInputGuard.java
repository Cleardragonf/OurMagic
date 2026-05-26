package com.ourmagic.client;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ClientGuiInputGuard {
    private ClientGuiInputGuard() {
    }

    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !isOurMagicScreen(event.getScreen())) {
            return;
        }

        KeyMapping swapOffhand = minecraft.options.keySwapOffhand;
        if (!swapOffhand.matches(event.getKeyCode(), event.getScanCode()) || !isHoldingWand(minecraft)) {
            return;
        }

        while (swapOffhand.consumeClick()) {
            // Drain queued offhand swaps so vanilla cannot move the GUI wand into equipment slots.
        }
        swapOffhand.setDown(false);
        event.setCanceled(true);
    }

    private static boolean isOurMagicScreen(Screen screen) {
        return screen instanceof WandScreen
                || screen instanceof SpellcraftScreen
                || screen instanceof PlayerUpgradeScreen
                || screen instanceof GuideBookScreen;
    }

    private static boolean isHoldingWand(Minecraft minecraft) {
        return isWand(minecraft.player.getMainHandItem()) || isWand(minecraft.player.getOffhandItem());
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }
}
