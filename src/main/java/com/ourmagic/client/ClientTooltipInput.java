package com.ourmagic.client;

import com.ourmagic.OurMagic;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ClientTooltipInput {
    private static int scrollOffset;

    private ClientTooltipInput() {
    }

    public static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public static int scrollOffset() {
        return scrollOffset;
    }

    @SubscribeEvent
    public static void mouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (!Screen.hasShiftDown()) {
            scrollOffset = 0;
            return;
        }

        double delta = event.getScrollDelta();
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (delta < 0) {
            scrollOffset++;
        }
    }
}
