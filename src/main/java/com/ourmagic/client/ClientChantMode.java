package com.ourmagic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ourmagic.OurMagic;
import com.ourmagic.network.ChantCastPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
    private static final float LOOK_THRESHOLD_DEGREES = 8.0F;
    private static final RandomSource RANDOM = RandomSource.create();
    private static boolean active;
    private static InteractionHand hand = InteractionHand.MAIN_HAND;
    private static Direction prompt = Direction.UP;
    private static int progress;
    private static int requiredProgress = 1;
    private static int streak;
    private static float multiplier = 1.0F;
    private static float lastYaw;
    private static float lastPitch;
    private static float accumulatedYaw;
    private static float accumulatedPitch;

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

        if (minecraft.screen != null || !minecraft.options.keyUse.isDown() || !altDown(minecraft) || !isWand(minecraft.player.getItemInHand(hand))) {
            finish();
            return;
        }

        Direction gesture = updateLookGesture(minecraft);
        if (gesture != Direction.NONE) {
            if (gesture != prompt) {
                cancel();
                return;
            }

            progress++;
            resetLookGesture(minecraft);
            if (progress >= requiredProgress) {
                progress = 0;
                streak++;
                multiplier = Math.min(100.0F, (float) Math.pow(2.0D, streak));
            }
            prompt = randomPromptExcept(prompt);
        }
    }

    private static Direction updateLookGesture(Minecraft minecraft) {
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        accumulatedYaw += Mth.wrapDegrees(yaw - lastYaw);
        accumulatedPitch += pitch - lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;

        float absYaw = Math.abs(accumulatedYaw);
        float absPitch = Math.abs(accumulatedPitch);
        if (Math.max(absYaw, absPitch) < LOOK_THRESHOLD_DEGREES) {
            return Direction.NONE;
        }

        if (absPitch >= absYaw) {
            return accumulatedPitch < 0.0F ? Direction.UP : Direction.DOWN;
        }
        return accumulatedYaw < 0.0F ? Direction.LEFT : Direction.RIGHT;
    }

    private static void resetLookGesture(Minecraft minecraft) {
        lastYaw = minecraft.player.getYRot();
        lastPitch = minecraft.player.getXRot();
        accumulatedYaw = 0.0F;
        accumulatedPitch = 0.0F;
    }

    private static Direction randomPromptExcept(Direction previous) {
        Direction next;
        do {
            next = Direction.PROMPTS[RANDOM.nextInt(Direction.PROMPTS.length)];
        } while (next == previous && Direction.PROMPTS.length > 1);
        return next;
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
        String text = prompt.symbol;
        graphics.drawString(minecraft.font, Component.literal(text), centerX - minecraft.font.width(text) / 2, centerY - 19, 0xFFE9D7FF, true);

        String progressText = progressBar();
        graphics.drawString(minecraft.font, Component.literal(progressText), centerX - minecraft.font.width(progressText) / 2, centerY - 4, 0xFF66FFAA, true);

        String power = String.format("x%.1f", multiplier);
        graphics.drawString(minecraft.font, Component.literal(power), centerX - minecraft.font.width(power) / 2, centerY + 10, 0xFFFFD84D, true);
    }

    private static void start(InteractionHand castHand) {
        Minecraft minecraft = Minecraft.getInstance();
        active = true;
        hand = castHand;
        prompt = randomPromptExcept(Direction.NONE);
        resetLookGesture(minecraft);
        progress = 0;
        requiredProgress = requiredProgress(minecraft.player.getItemInHand(castHand));
        streak = 0;
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

    private static void cancel() {
        active = false;
        progress = 0;
        streak = 0;
        multiplier = 1.0F;
    }

    private static int requiredProgress(ItemStack stack) {
        return Math.max(1, WandData.read(stack).activeSpellLevel());
    }

    private static String progressBar() {
        int filled = Math.round((progress / (float) requiredProgress) * 12.0F);
        return "[" + "#".repeat(Math.max(0, filled)) + "-".repeat(Math.max(0, 12 - filled)) + "]";
    }

    private static boolean altDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private enum Direction {
        NONE(""),
        UP("^"),
        DOWN("v"),
        LEFT("<"),
        RIGHT(">");

        private static final Direction[] PROMPTS = {UP, DOWN, LEFT, RIGHT};
        private final String symbol;

        Direction(String symbol) {
            this.symbol = symbol;
        }
    }
}
