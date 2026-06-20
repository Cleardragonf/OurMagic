package com.ourmagic.client;

import com.ourmagic.block.entity.ArcaneQuarryBlockEntity;
import com.ourmagic.ui.ArcaneQuarryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ArcaneQuarryScreen extends AbstractContainerScreen<ArcaneQuarryMenu> {
    private static final int WIDTH = 230;
    private static final int HEIGHT = 122;

    public ArcaneQuarryScreen(ArcaneQuarryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = HEIGHT + 100;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE100819);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, 0xFF7A45B5);
        graphics.fill(leftPos + 14, topPos + 36, leftPos + imageWidth - 14, topPos + 54, 0xAA05030A);
        graphics.fill(leftPos + 14, topPos + 66, leftPos + imageWidth - 14, topPos + 98, 0xAA05030A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, "Arcane Quarry", imageWidth / 2, 12, 0xFFEBD6FF);
        ArcaneQuarryBlockEntity quarry = menu.quarry();
        if (quarry == null) {
            graphics.drawString(font, "Quarry unavailable", 18, 42, 0xFFFF7777, false);
            return;
        }
        int stored = quarry.storedEnergy();
        int capacity = quarry.capacity();
        int fill = Math.min(imageWidth - 32, Math.round((imageWidth - 32) * stored / (float) capacity));
        graphics.fill(16, 38, 16 + fill, 52, 0xFF55E8FF);
        graphics.drawCenteredString(font, stored + "/" + capacity + " MF", imageWidth / 2, 41, 0xFFFFFFFF);
        graphics.drawString(font, quarry.statusLine(), 18, 70, 0xFFBFA7D8, false);
        graphics.drawString(font, "Shift-right-click block to bind nearest two markers.", 18, 84, 0xFF8F7BA8, false);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
