package com.ourmagic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ourmagic.OurMagic;
import com.ourmagic.network.ChantCastPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ClientChantMode {
    private static final int SEGMENT_TICKS = 8;
    private static boolean active;
    private static InteractionHand hand = InteractionHand.MAIN_HAND;
    private static int ticks;
    private static int streak;
    private static int lastBonusSegment = -1;
    private static float multiplier = 1.0F;

    private ClientChantMode() {
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        ItemStack stack = minecraft.player.getItemInHand(event.getHand());
        if (!isWand(stack)) {
            return;
        }

        if (active || altDown(minecraft)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            if (!active && altDown(minecraft)) {
                start(event.getHand());
            }
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (minecraft.screen != null || !minecraft.options.keyUse.isDown() || !isWand(minecraft.player.getItemInHand(hand))) {
            finish();
            return;
        }

        ticks++;
        int segment = ticks / SEGMENT_TICKS;
        if (currentNumber() == targetNumber(segment) && segment != lastBonusSegment) {
            lastBonusSegment = segment;
            if (altDown(minecraft)) {
                streak++;
                multiplier = Math.min(100.0F, (float) Math.pow(2.0D, streak));
            } else {
                streak = 0;
                multiplier = 1.0F;
            }
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight) {
        if (!active) {
            return;
        }

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int number = currentNumber();
        int target = targetNumber(ticks / SEGMENT_TICKS);
        boolean bonus = number == target;
        int color = bonus ? 0xFF66FFAA : 0xFFE9D7FF;
        String text = String.valueOf(number);
        graphics.drawString(minecraft.font, Component.literal(text), centerX - minecraft.font.width(text) / 2, centerY - 19, color, true);

        String power = String.format("x%.1f", multiplier);
        graphics.drawString(minecraft.font, Component.literal(power), centerX - minecraft.font.width(power) / 2, centerY + 10, 0xFFFFD84D, true);
    }

    private static void start(InteractionHand castHand) {
        active = true;
        hand = castHand;
        ticks = 0;
        streak = 0;
        lastBonusSegment = -1;
        multiplier = 1.0F;
    }

    private static void finish() {
        if (!active) {
            return;
        }

        InteractionHand castHand = hand;
        float castMultiplier = multiplier;
        active = false;
        multiplier = 1.0F;
        ModNetwork.CHANNEL.sendToServer(new ChantCastPacket(castHand, castMultiplier));
    }

    private static int currentNumber() {
        return 1 + Math.floorMod(ticks / SEGMENT_TICKS * 7 + 3, 9);
    }

    private static int targetNumber(int segment) {
        return 1 + Math.floorMod(segment * 5 + 6, 9);
    }

    private static boolean altDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }
}
