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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mouseScrolled(InputEvent.MouseScrollingEvent event) {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void keyPressed(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (minecraft.screen instanceof ChantScreen) {
            return;
        }

        if (minecraft.screen == null && event.getKey() == GLFW.GLFW_KEY_C && altDown(minecraft)) {
            InteractionHand wandHand = activeWandHand(minecraft);
            if (wandHand == null) {
                return;
            }
            if (charged) {
                send(ChantingPacket.Mode.CANCEL);
            }
            start(minecraft, wandHand);
            minecraft.setScreen(new ChantScreen());
            cancelEvent(event);
        }
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
            if (minecraft.screen instanceof ChantScreen) {
                minecraft.setScreen(null);
            }
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

    private static final class ChantScreen extends Screen {
        private static final int PANEL_W = 248;
        private static final int PANEL_H = 118;
        private boolean closingFromAction;

        private ChantScreen() {
            super(Component.literal("Chant"));
        }

        @Override
        protected void init() {
            int x = (width - PANEL_W) / 2;
            int y = (height - PANEL_H) / 2;
            addRenderableWidget(Button.builder(Component.literal("X"), button -> closeAndCancel())
                    .bounds(x + PANEL_W - 22, y + 6, 16, 16)
                    .build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int x = (width - PANEL_W) / 2;
            int y = (height - PANEL_H) / 2;
            graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xEE0B0812);
            graphics.fill(x, y, x + PANEL_W, y + 1, 0xFFC66CFF);
            graphics.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, 0xFF6F4A96);
            graphics.fill(x, y, x + 1, y + PANEL_H, 0xFF6F4A96);
            graphics.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, 0xFF6F4A96);

            String title = "Chanting " + level + "/" + MAX_LEVEL;
            graphics.drawCenteredString(font, title, width / 2, y + 14, 0xFFFFD84D);

            int split = Math.min(typed.length(), prompt.length());
            String shown = prompt.substring(0, split).toUpperCase(Locale.ROOT) + prompt.substring(split);
            graphics.drawCenteredString(font, shown, width / 2, y + 45, 0xFFE9D7FF);

            int barW = 168;
            int barX = x + (PANEL_W - barW) / 2;
            int barY = y + 66;
            graphics.fill(barX, barY, barX + barW, barY + 8, 0xFF181321);
            int fill = Math.round(barW * (progress / (float) Math.max(1, wordsPerLevel)));
            graphics.fill(barX, barY, barX + fill, barY + 8, 0xFF66FFAA);
            graphics.drawCenteredString(font, "Enter locks charge", width / 2, y + 86, 0xFF9D8FB1);

            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeAndCancel();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                closingFromAction = true;
                lockCharge();
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!typed.isEmpty()) {
                    typed = typed.substring(0, typed.length() - 1);
                }
                return true;
            }
            char letter = letter(keyCode);
            if (letter != 0) {
                typed += letter;
                if (!prompt.startsWith(typed)) {
                    typed = "";
                    progress = Math.max(0, progress - 1);
                } else if (typed.equals(prompt)) {
                    completeWord();
                    if (!active) {
                        closingFromAction = true;
                        Minecraft.getInstance().setScreen(null);
                    }
                }
                return true;
            }
            return true;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button) || true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            return true;
        }

        @Override
        public void onClose() {
            closeAndCancel();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private void closeAndCancel() {
            if (!closingFromAction) {
                cancel();
            }
            closingFromAction = true;
            Minecraft.getInstance().setScreen(null);
        }
    }
}
