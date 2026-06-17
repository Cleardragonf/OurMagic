package com.ourmagic.client;

import com.ourmagic.OurMagic;
import com.ourmagic.network.MagicLinkHudQueryPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MagicLinkHudOverlay {
    private static BlockPos lastRequested;
    private static long nextRequestAt;

    private MagicLinkHudOverlay() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("magic_link_hud", MagicLinkHudOverlay::render);
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || !holdingHudTool(minecraft)) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos target = blockHit.getBlockPos();
        long gameTime = minecraft.level.getGameTime();
        if ((!target.equals(lastRequested) || gameTime >= nextRequestAt) && minecraft.player.distanceToSqr(target.getCenter()) <= 64.0D) {
            lastRequested = target.immutable();
            nextRequestAt = gameTime + 5;
            ModNetwork.CHANNEL.sendToServer(new MagicLinkHudQueryPacket(target));
        }

        if (!MagicLinkHudData.active(target, gameTime)) {
            return;
        }

        renderPanel(graphics, minecraft, screenWidth, MagicLinkHudData.title(), MagicLinkHudData.lines());
    }

    private static boolean holdingHudTool(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack off = minecraft.player.getOffhandItem();
        return main.is(ModItems.MAGIC_LINKER.get()) || off.is(ModItems.MAGIC_LINKER.get())
                || main.is(ModItems.WARD_TUNER.get()) || off.is(ModItems.WARD_TUNER.get());
    }

    private static void renderPanel(GuiGraphics graphics, Minecraft minecraft, int screenWidth, String title, List<String> lines) {
        int width = Math.max(150, minecraft.font.width(title) + 22);
        for (String line : lines) {
            width = Math.max(width, line.startsWith("@bar|") ? 190 : minecraft.font.width(line) + 22);
        }
        width = Math.min(310, width);
        int height = 20;
        for (String line : lines) {
            height += line.startsWith("@bar|") ? 18 : 11;
        }
        int x = (screenWidth - width) / 2;
        int y = 8;

        graphics.fill(x, y, x + width, y + height, 0xCC070711);
        graphics.fill(x, y, x + width, y + 1, 0xFFC66CFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xAA000000);
        graphics.drawCenteredString(minecraft.font, Component.literal(title), screenWidth / 2, y + 5, 0xFFEBD6FF);
        int lineY = y + 18;
        for (String line : lines) {
            if (line.startsWith("@bar|")) {
                renderProgressBar(graphics, minecraft, x + 10, lineY, width - 20, line);
                lineY += 18;
            } else {
                graphics.drawString(minecraft.font, Component.literal(line), x + 10, lineY, 0xFFD7C3E8, false);
                lineY += 11;
            }
        }
    }

    private static void renderProgressBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String encoded) {
        String[] parts = encoded.split("\\|", 4);
        if (parts.length != 4) {
            return;
        }
        int value = parseInt(parts[2]);
        int max = Math.max(1, parseInt(parts[3]));
        int filled = Math.max(0, Math.min(width, width * Math.min(value, max) / max));
        String label = parts[1] + " " + value + "/" + max;

        graphics.drawString(minecraft.font, Component.literal(label), x, y, 0xFFD7C3E8, false);
        int barY = y + 10;
        graphics.fill(x, barY, x + width, barY + 6, 0xAA1A1024);
        graphics.fill(x + 1, barY + 1, x + width - 1, barY + 5, 0xFF281735);
        if (filled > 2) {
            graphics.fill(x + 1, barY + 1, x + filled - 1, barY + 5, 0xFF55E8FF);
            graphics.fill(x + 1, barY + 1, x + filled - 1, barY + 2, 0xFFB8F6FF);
        }
        graphics.fill(x, barY, x + width, barY + 1, 0xFFC66CFF);
        graphics.fill(x, barY + 5, x + width, barY + 6, 0xAA000000);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
