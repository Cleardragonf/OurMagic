package com.ourmagic.client;

import com.ourmagic.block.entity.ArcaneQuarryBlockEntity;
import com.ourmagic.network.ArcaneQuarryControlPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.ui.ArcaneQuarryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ArcaneQuarryScreen extends AbstractContainerScreen<ArcaneQuarryMenu> {
    private static final int WIDTH = 230;
    private static final int HEIGHT = 218;
    private Button pauseButton;

    public ArcaneQuarryScreen(ArcaneQuarryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 34;
        inventoryLabelY = 98;
    }

    @Override
    protected void init() {
        super.init();
        pauseButton = addRenderableWidget(Button.builder(Component.literal("Pause"), button ->
                ModNetwork.CHANNEL.sendToServer(new ArcaneQuarryControlPacket(ArcaneQuarryControlPacket.Action.TOGGLE_PAUSE)))
                .bounds(leftPos + 16, topPos + 54, 64, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Bind"), button ->
                ModNetwork.CHANNEL.sendToServer(new ArcaneQuarryControlPacket(ArcaneQuarryControlPacket.Action.BIND_MARKERS)))
                .bounds(leftPos + 84, topPos + 54, 64, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), button ->
                ModNetwork.CHANNEL.sendToServer(new ArcaneQuarryControlPacket(ArcaneQuarryControlPacket.Action.RESET_AREA)))
                .bounds(leftPos + 152, topPos + 54, 62, 18)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE100819);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, 0xFF7A45B5);
        graphics.fill(leftPos + 14, topPos + 36, leftPos + imageWidth - 14, topPos + 54, 0xAA05030A);
        graphics.fill(leftPos + 14, topPos + 76, leftPos + imageWidth - 14, topPos + 96, 0xAA05030A);
        for (int i = 0; i < 7; i++) {
            int x = leftPos + 33 + i * 24;
            graphics.fill(x, topPos + 77, x + 18, topPos + 95, 0xFF1D102A);
            drawBorder(graphics, x, topPos + 77, 18, 18, 0x664A2F68);
        }
        graphics.fill(leftPos + 30, topPos + 106, leftPos + 200, topPos + 188, 0xAA05030A);
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
        pauseButton.setMessage(Component.literal(quarry.paused() ? "Resume" : "Pause"));
        graphics.drawString(font, "Upgrades", 18, 83, 0xFFBFA7D8, false);
        graphics.drawString(font, quarry.statusLine(), 18, 195, 0xFFBFA7D8, false);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
