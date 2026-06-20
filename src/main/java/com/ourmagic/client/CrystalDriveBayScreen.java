package com.ourmagic.client;

import com.ourmagic.block.entity.CrystalDriveBayBlockEntity;
import com.ourmagic.item.CrystalDriveItem;
import com.ourmagic.ui.CrystalDriveBayMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CrystalDriveBayScreen extends AbstractContainerScreen<CrystalDriveBayMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    public CrystalDriveBayScreen(CrystalDriveBayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE100819);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, 0xFF55E8FF);
        graphics.fill(leftPos + 16, topPos + 22, leftPos + 160, topPos + 62, 0xAA05030A);
        for (int i = 0; i < 4; i++) {
            int x = leftPos + 52 + i * 24;
            graphics.fill(x, topPos + 34, x + 18, topPos + 52, 0xFF1D102A);
            drawBorder(graphics, x, topPos + 34, 18, 18, 0xFF7A45B5);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "Crystal Drive Bay", 8, 8, 0xFFEBD6FF, false);
        CrystalDriveBayBlockEntity bay = menu.bay();
        graphics.drawString(font, bay == null ? "No bay" : bay.statusLine(), 18, 24, 0xFFBFA7D8, false);
        for (int i = 0; i < 4 && bay != null; i++) {
            ItemStack drive = bay.driveSlots().getStackInSlot(i);
            if (drive.getItem() instanceof CrystalDriveItem crystalDrive) {
                int used = CrystalDriveItem.storedCount(drive);
                int fill = Math.min(16, Math.round(16.0F * used / crystalDrive.capacity()));
                graphics.fill(54 + i * 24, 55, 54 + i * 24 + fill, 57, 0xFF55E8FF);
            }
        }
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFFEBD6FF, false);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
