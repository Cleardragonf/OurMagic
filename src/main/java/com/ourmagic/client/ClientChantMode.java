package com.ourmagic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ourmagic.OurMagic;
import com.ourmagic.network.ChantingPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ClientChantMode {
    private static final String[] WORDS = {
            "aelir", "vohru", "kaelis", "nyr", "solun", "ithra", "maeven", "oruun",
            "velis", "shael", "koru", "thalen", "ezra", "luneth", "vaari", "omryn"
    };
    private static final int MAX_LEVEL = 5;
    private static final int CHARGE_TTL_TICKS = 20 * 45;
    private static final RandomSource RANDOM = RandomSource.create();

    private static boolean active;
    private static boolean charged;
    private static InteractionHand hand = InteractionHand.MAIN_HAND;
    private static String prompt = WORDS[0];
    private static String typed = "";
    private static int progress;
    private static int level;
    private static int wordsPerLevel = 4;
    private static int chargedTicks;

    private ClientChantMode() {
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active) {
            Minecraft minecraft = Minecraft.getInstance();
            if (event.isUseItem()
                    && minecraft.player != null
                    && hasWand(minecraft.player.getItemInHand(event.getHand()))) {
                finishForCast();
                return;
            }
            if (event.isCancelable()) {
                event.setCanceled(true);
            }
            return;
        }

        if (!charged || !event.isUseItem()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && hasWand(minecraft.player.getItemInHand(event.getHand()))) {
            charged = false;
            chargedTicks = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mouseClicked(InputEvent.MouseButton.Pre event) {
        if (!active) {
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && activeWandHand(minecraft) != null) {
                finishForCast();
                return;
            }
        }
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (!active) {
            return;
        }
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void keyPressed(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (active && event.getKey() == GLFW.GLFW_KEY_C && altDown(minecraft)) {
            releaseBlockedKeyMappings(minecraft);
            lockCharge();
            cancelEvent(event);
            return;
        }

        if (!active && event.getKey() == GLFW.GLFW_KEY_C && altDown(minecraft)) {
            InteractionHand wandHand = activeWandHand(minecraft);
            if (wandHand == null) {
                return;
            }
            if (charged) {
                send(ChantingPacket.Mode.CANCEL);
            }
            start(minecraft, wandHand);
            cancelEvent(event);
            return;
        }

        if (!active) {
            return;
        }

        if (isSpellSlotKey(event.getKey())) {
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE) {
            releaseBlockedKeyMappings(minecraft);
            cancel();
            cancelEvent(event);
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_ENTER || event.getKey() == GLFW.GLFW_KEY_KP_ENTER) {
            lockCharge();
            cancelEvent(event);
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!typed.isEmpty()) {
                typed = typed.substring(0, typed.length() - 1);
            }
            cancelEvent(event);
            return;
        }

        char letter = letter(event.getKey());
        if (letter != 0) {
            typed += letter;
            if (!prompt.startsWith(typed)) {
                typed = "";
                progress = Math.max(0, progress - 1);
            } else if (typed.equals(prompt)) {
                completeWord();
            }
            releaseBlockedKeyMappings(minecraft);
            cancelEvent(event);
            return;
        }

        cancelEvent(event);
        releaseBlockedKeyMappings(minecraft);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            cancelLocal();
            return;
        }

        if (active && !hasWand(minecraft.player.getItemInHand(hand))) {
            cancel();
            return;
        }

        if (active) {
            releaseBlockedKeyMappings(minecraft);
        }

        if (charged && ++chargedTicks > CHARGE_TTL_TICKS) {
            cancel();
        }
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight) {
        if (!active && !charged) {
            return;
        }

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2 + 28;
        String title = active ? "Chanting " + level + "/" + MAX_LEVEL : "Chant Ready " + level + "/" + MAX_LEVEL;
        graphics.drawString(minecraft.font, Component.literal(title), centerX - minecraft.font.width(title) / 2, centerY, 0xFFFFD84D, true);

        if (active) {
            int split = Math.min(typed.length(), prompt.length());
            String shown = prompt.substring(0, split).toUpperCase(Locale.ROOT) + prompt.substring(split);
            graphics.drawString(minecraft.font, Component.literal(shown), centerX - minecraft.font.width(shown) / 2, centerY + 12, 0xFFE9D7FF, true);

            String bar = "[" + "#".repeat(progress) + "-".repeat(Math.max(0, wordsPerLevel - progress)) + "]";
            graphics.drawString(minecraft.font, Component.literal(bar), centerX - minecraft.font.width(bar) / 2, centerY + 24, 0xFF66FFAA, true);
        }
    }

    private static void start(Minecraft minecraft, InteractionHand wandHand) {
        active = true;
        charged = false;
        hand = wandHand;
        typed = "";
        progress = 0;
        level = 0;
        chargedTicks = 0;
        wordsPerLevel = wordsPerLevel(minecraft.player.getItemInHand(wandHand));
        prompt = randomWord("");
        send(ChantingPacket.Mode.START);
    }

    private static void completeWord() {
        progress++;
        typed = "";
        prompt = randomWord(prompt);
        if (progress >= wordsPerLevel) {
            progress = 0;
            level = Math.min(MAX_LEVEL, level + 1);
            send(ChantingPacket.Mode.UPDATE);
            if (level >= MAX_LEVEL) {
                lockCharge();
            }
        }
    }

    private static void lockCharge() {
        if (level <= 0) {
            cancel();
            return;
        }
        active = false;
        charged = true;
        chargedTicks = 0;
        send(ChantingPacket.Mode.UPDATE);
    }

    private static void cancel() {
        send(ChantingPacket.Mode.CANCEL);
        cancelLocal();
    }

    private static void cancelLocal() {
        active = false;
        charged = false;
        typed = "";
        progress = 0;
        level = 0;
        chargedTicks = 0;
    }

    private static void finishForCast() {
        if (level <= 0) {
            send(ChantingPacket.Mode.CANCEL);
        }
        active = false;
        charged = false;
        typed = "";
        progress = 0;
        chargedTicks = 0;
    }

    private static void send(ChantingPacket.Mode mode) {
        ModNetwork.CHANNEL.sendToServer(new ChantingPacket(mode, hand, level));
    }

    private static int wordsPerLevel(ItemStack stack) {
        int manaCost = WandData.read(stack).activeManaCost();
        return Math.max(2, Math.min(6, 7 - manaCost / 12));
    }

    private static String randomWord(String previous) {
        String next;
        do {
            next = WORDS[RANDOM.nextInt(WORDS.length)];
        } while (next.equals(previous) && WORDS.length > 1);
        return next;
    }

    private static InteractionHand activeWandHand(Minecraft minecraft) {
        if (hasWand(minecraft.player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        return hasWand(minecraft.player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }

    private static boolean hasWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private static boolean altDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static char letter(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return (char) ('a' + key - GLFW.GLFW_KEY_A);
        }
        return 0;
    }

    private static boolean isSpellSlotKey(int key) {
        return key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_6;
    }

    private static void releaseBlockedKeyMappings(Minecraft minecraft) {
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            while (mapping.consumeClick()) {
                // Drain queued keybind clicks so menus, movement, and other actions do not fire during chanting.
            }
            mapping.setDown(false);
        }
    }

    private static void cancelEvent(InputEvent.Key event) {
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }
}
