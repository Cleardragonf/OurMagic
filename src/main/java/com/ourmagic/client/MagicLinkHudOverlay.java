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
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || !holdingLinker(minecraft)) {
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

    private static boolean holdingLinker(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack off = minecraft.player.getOffhandItem();
        return main.is(ModItems.MAGIC_LINKER.get()) || off.is(ModItems.MAGIC_LINKER.get());
    }

    private static void renderPanel(GuiGraphics graphics, Minecraft minecraft, int screenWidth, String title, List<String> lines) {
        int width = Math.max(150, minecraft.font.width(title) + 22);
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line) + 22);
        }
        width = Math.min(310, width);
        int height = 20 + lines.size() * 11;
        int x = (screenWidth - width) / 2;
        int y = 8;

        graphics.fill(x, y, x + width, y + height, 0xCC070711);
        graphics.fill(x, y, x + width, y + 1, 0xFFC66CFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xAA000000);
        graphics.drawCenteredString(minecraft.font, Component.literal(title), screenWidth / 2, y + 5, 0xFFEBD6FF);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(minecraft.font, Component.literal(lines.get(i)), x + 10, y + 18 + i * 11, 0xFFD7C3E8, false);
        }
    }
}
