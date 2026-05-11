package com.ourmagic.client;

import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.PlayerUpgradePacket;
import com.ourmagic.ui.PlayerUpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PlayerUpgradeScreen extends AbstractContainerScreen<PlayerUpgradeMenu> {
    private static final int WIDTH = 230;
    private static final int HEIGHT = 140;
    private static final int COLOR_FRAME = 0xFF7A45B5;
    private static final int COLOR_PANEL = 0xDD120A1D;
    private static final int COLOR_TEXT = 0xFFE8D8FF;
    private static final int COLOR_MANA = 0xFF5BD6FF;
    private static final int COLOR_REGEN = 0xFF79FF8B;

    private Button maxManaButton;
    private Button regenButton;

    public PlayerUpgradeScreen(PlayerUpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = HEIGHT + 100;
    }

    @Override
    protected void init() {
        super.init();
        maxManaButton = addRenderableWidget(Button.builder(Component.empty(), b -> upgrade(PlayerUpgradePacket.MAX_MANA))
                .bounds(leftPos + 18, topPos + 94, 90, 20)
                .build());
        regenButton = addRenderableWidget(Button.builder(Component.empty(), b -> upgrade(PlayerUpgradePacket.MANA_REGEN))
                .bounds(leftPos + 122, topPos + 94, 90, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOR_PANEL);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, COLOR_FRAME);
        graphics.fill(leftPos + 14, topPos + 34, leftPos + 108, topPos + 88, 0xAA071320);
        graphics.fill(leftPos + 122, topPos + 34, leftPos + 216, topPos + 88, 0xAA0A1A10);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, "Player Upgrades", imageWidth / 2, 12, COLOR_TEXT);
        graphics.drawCenteredString(font, "Magic L" + ClientManaData.magicLevel() + "  XP "
                + ClientManaData.magicXp() + "/" + ClientManaData.magicXpToNextLevel()
                + "  Points " + ClientManaData.magicPoints(), imageWidth / 2, 24, 0xFFFFD86A);

        graphics.drawCenteredString(font, "Max Mana", 61, 43, COLOR_MANA);
        graphics.drawCenteredString(font, ClientManaData.maxMana() + " -> " + (ClientManaData.maxMana() + 10), 61, 59, COLOR_TEXT);
        graphics.drawCenteredString(font, "+ larger spell pool", 61, 73, 0xFFB9A6D6);

        graphics.drawCenteredString(font, "Mana Regen", 169, 43, COLOR_REGEN);
        graphics.drawCenteredString(font, ClientManaData.regen() + "/s -> " + (ClientManaData.regen() + 1) + "/s", 169, 59, COLOR_TEXT);
        graphics.drawCenteredString(font, "+ faster recovery", 169, 73, 0xFFB9A6D6);

        updateButton(maxManaButton, PlayerUpgradePacket.MAX_MANA, "+10 Mana");
        updateButton(regenButton, PlayerUpgradePacket.MANA_REGEN, "+1 Regen");
    }

    private void updateButton(Button button, String upgrade, String label) {
        int cost = PlayerUpgradePacket.cost(upgrade);
        button.setMessage(Component.literal(label + " (" + cost + "P)"));
        button.active = minecraft != null && minecraft.player != null
                && (minecraft.player.getAbilities().instabuild || ClientManaData.magicPoints() >= cost);
        button.setTooltip(Tooltip.create(Component.literal("Costs " + cost + " magic point" + (cost == 1 ? "" : "s"))));
    }

    private void upgrade(String upgrade) {
        ModNetwork.CHANNEL.sendToServer(new PlayerUpgradePacket(upgrade));
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
