package com.ourmagic.client;

import com.ourmagic.OurMagic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ScryHudOverlay {
    private ScryHudOverlay() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("scry_hud", ScryHudOverlay::render);
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientScryData.active()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        float scale = ScreenScale.forDesign(minecraft, screenWidth, screenHeight, 260, 58);
        int virtualScreenWidth = Math.max(1, Math.round(screenWidth / scale));
        int virtualScreenHeight = Math.max(1, Math.round(screenHeight / scale));
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);

        int width = 236;
        int x = (virtualScreenWidth - width) / 2;
        int y = virtualScreenHeight - 58;
        graphics.fill(x - 5, y - 7, x + width + 5, y + 36, 0xAA050510);
        graphics.fill(x - 5, y - 7, x + width + 5, y - 6, 0xFF8C55CC);

        String target = ClientScryData.targetName();
        String count = ClientScryData.targetCount() <= 0 ? "" : " " + (ClientScryData.targetIndex() + 1) + "/" + ClientScryData.targetCount();
        graphics.drawCenteredString(minecraft.font, "Scrying: " + target + count, virtualScreenWidth / 2, y - 2, 0xFFE8D8FF);
        graphics.drawCenteredString(minecraft.font, "1 Prev   2 Next   3 Renew   4 Exit", virtualScreenWidth / 2, y + 12, 0xFFFFD86A);
        graphics.drawCenteredString(minecraft.font, "Time " + String.format("%.1fs", ClientScryData.ticksRemaining() / 20.0F), virtualScreenWidth / 2, y + 24, 0xFF80D8FF);
        graphics.pose().popPose();
    }
}
