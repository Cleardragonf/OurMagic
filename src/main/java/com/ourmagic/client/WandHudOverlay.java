package com.ourmagic.client;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WandHudOverlay {
    private static final int WIDTH = 144;
    private static final int BAR_HEIGHT = 7;
    private static final int PADDING = 5;

    private WandHudOverlay() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("wand_hud", WandHudOverlay::render);
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        ItemStack wand = activeWand(minecraft);
        if (wand.isEmpty()) {
            return;
        }

        WandData data = WandData.read(wand);
        int spellCost = data.activeManaCost();

        int x = (screenWidth - WIDTH) / 2;
        int y = screenHeight - 61;
        int panelHeight = 23;
        graphics.fill(x - PADDING, y - PADDING, x + WIDTH + PADDING, y + panelHeight, 0x99000000);

        int manaWidth = fillWidth(ClientManaData.mana(), ClientManaData.maxMana());
        graphics.drawString(minecraft.font, Component.literal(data.activeSpellName()), x, y, 0xFFE6D6FF, false);
        graphics.drawString(minecraft.font, Component.literal("Cost " + spellCost), x + WIDTH - 38, y, 0xFFC7E8FF, false);

        int manaY = y + 12;
        drawBar(graphics, x, manaY, WIDTH, BAR_HEIGHT, 0xFF172B45, 0xFF2D8CFF, manaWidth);
        graphics.drawString(minecraft.font, Component.literal("Mana " + ClientManaData.mana() + "/" + ClientManaData.maxMana() + "  +" + ClientManaData.regen() + "/s"), x + 3, manaY - 1, 0xFFFFFFFF, false);

        renderHotbarCooldowns(graphics, minecraft, screenWidth, screenHeight);
        ClientChantMode.render(graphics, minecraft, screenWidth, screenHeight);
    }

    private static ItemStack activeWand(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (isWand(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = minecraft.player.getOffhandItem();
        return isWand(offHand) ? offHand : ItemStack.EMPTY;
    }

    private static int fillWidth(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(WIDTH, Math.round(WIDTH * (value / (float) max))));
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, int background, int foreground, int fillWidth) {
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + fillWidth, y + height, foreground);
        graphics.fill(x, y, x + width, y + 1, 0x66FFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xAA000000);
    }

    private static void renderHotbarCooldowns(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight) {
        Inventory inventory = minecraft.player.getInventory();
        long gameTime = minecraft.level.getGameTime();
        int hotbarLeft = screenWidth / 2 - 91;
        int hotbarTop = screenHeight - 22;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isWand(stack)) {
                continue;
            }

            WandData data = WandData.read(stack);
            int cooldownTicks = data.activeCooldownTicks();
            if (cooldownTicks <= 0) {
                continue;
            }

            long remaining = data.cooldownUntil() - gameTime;
            if (remaining <= 0) {
                continue;
            }

            float progress = Math.max(0.0F, Math.min(1.0F, remaining / (float) cooldownTicks));
            int alpha = 70 + Math.round(145 * progress);
            int green = Math.round(48 * (1.0F - progress));
            int overlayColor = (alpha << 24) | (0xFF << 16) | (green << 8);
            int slotX = hotbarLeft + slot * 20 + 2;
            int slotY = hotbarTop + 2;
            int cooldownHeight = Math.max(1, Math.round(16 * progress));

            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, overlayColor);
            graphics.fill(slotX, slotY + 16 - cooldownHeight, slotX + 16, slotY + 16, 0x66FF0000);

            String seconds = remaining >= 20 ? String.valueOf((int) Math.ceil(remaining / 20.0F)) : String.format("%.1f", remaining / 20.0F);
            int textX = slotX + 8 - minecraft.font.width(seconds) / 2;
            int textY = slotY + 4;
            graphics.drawString(minecraft.font, seconds, textX + 1, textY + 1, 0xCC000000, false);
            graphics.drawString(minecraft.font, seconds, textX, textY, 0xFFFFFFFF, false);
        }
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }
}
