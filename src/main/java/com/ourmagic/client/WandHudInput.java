package com.ourmagic.client;

import com.ourmagic.OurMagic;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class WandHudInput {
    private WandHudInput() {
    }

    @SubscribeEvent
    public static void mouseClicked(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        if (WandHudOverlay.mouseClicked(mouseX, mouseY, event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mouseScrolled(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (WandHudOverlay.mouseScrolled(event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void keyPressed(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        int slot = switch (event.getKey()) {
            case GLFW.GLFW_KEY_1 -> 0;
            case GLFW.GLFW_KEY_2 -> 1;
            case GLFW.GLFW_KEY_3 -> 2;
            case GLFW.GLFW_KEY_4 -> 3;
            case GLFW.GLFW_KEY_5 -> 4;
            case GLFW.GLFW_KEY_6 -> 5;
            default -> -1;
        };
        if (slot >= 0 && WandHudOverlay.selectAssignedSlot(slot)) {
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }
}
