package com.ourmagic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ourmagic.OurMagic;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.ScryCommandPacket;
import com.ourmagic.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class WandHudInput {
    private static boolean cursorReleasedForWand;

    private WandHudInput() {
    }

    @SubscribeEvent
    public static void mouseClicked(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            WandHudOverlay.mouseReleased(event.getButton());
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
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
        if (minecraft.screen != null || minecraft.player == null) {
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
        if (slot >= 0 && ClientScryData.active()) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                scryCommand(slot);
            }
            if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
                releaseMatchingKeyMappings(minecraft, event.getKey(), event.getScanCode());
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
            }
            return;
        }
        if (slot < 0 || !isHoldingWand(minecraft)) {
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS) {
            WandHudOverlay.selectAssignedSlot(slot);
        }
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
            releaseMatchingKeyMappings(minecraft, event.getKey(), event.getScanCode());
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldRelease = minecraft.screen == null
                && minecraft.player != null
                && minecraft.level != null
                && isHoldingWand(minecraft)
                && shiftDown(minecraft);

        if (shouldRelease && !cursorReleasedForWand) {
            minecraft.mouseHandler.releaseMouse();
            cursorReleasedForWand = true;
        } else if (!shouldRelease && cursorReleasedForWand) {
            minecraft.mouseHandler.grabMouse();
            cursorReleasedForWand = false;
        }

        WandHudOverlay.tickDrag();
    }

    private static boolean isHoldingWand(Minecraft minecraft) {
        return isWand(minecraft.player.getMainHandItem()) || isWand(minecraft.player.getOffhandItem());
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private static void scryCommand(int slot) {
        ScryCommandPacket.Command command = switch (slot) {
            case 0 -> ScryCommandPacket.Command.PREVIOUS;
            case 1 -> ScryCommandPacket.Command.NEXT;
            case 2 -> ScryCommandPacket.Command.RENEW;
            case 3 -> ScryCommandPacket.Command.EXIT;
            default -> null;
        };
        if (command != null) {
            ModNetwork.CHANNEL.sendToServer(new ScryCommandPacket(command));
        }
    }

    private static boolean shiftDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static void releaseMatchingKeyMappings(Minecraft minecraft, int key, int scanCode) {
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (mapping.matches(key, scanCode)) {
                while (mapping.consumeClick()) {
                    // Drain queued clicks so vanilla and other keybinds do not process 1-6 while a wand is held.
                }
                mapping.setDown(false);
            }
        }
    }
}
