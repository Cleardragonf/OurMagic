package com.ourmagic.client;

import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.ScryMarksPacket;
import com.ourmagic.network.ScrySelectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ScrySelectionScreen extends Screen {
    private static final int WIDTH = 220;
    private static final int ROW_H = 22;
    private final List<ScryMarksPacket.Entry> entries;

    private ScrySelectionScreen(List<ScryMarksPacket.Entry> entries) {
        super(Component.literal("Choose Scry Mark"));
        this.entries = List.copyOf(entries);
    }

    public static void open(List<ScryMarksPacket.Entry> entries) {
        Minecraft.getInstance().setScreen(new ScrySelectionScreen(entries));
    }

    @Override
    protected void init() {
        clearWidgets();
        int visible = Math.min(entries.size(), 8);
        int left = (width - WIDTH) / 2;
        int top = (height - (42 + visible * ROW_H)) / 2;
        for (int i = 0; i < visible; i++) {
            ScryMarksPacket.Entry entry = entries.get(i);
            addRenderableWidget(Button.builder(Component.literal(label(entry)), b -> {
                ModNetwork.CHANNEL.sendToServer(new ScrySelectPacket(entry.index()));
                onClose();
            }).bounds(left + 10, top + 30 + i * ROW_H, WIDTH - 20, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(left + WIDTH / 2 - 30, top + 34 + visible * ROW_H, 60, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int visible = Math.min(entries.size(), 8);
        int left = (width - WIDTH) / 2;
        int top = (height - (42 + visible * ROW_H)) / 2;
        graphics.fill(left, top, left + WIDTH, top + 48 + visible * ROW_H, 0xDD080812);
        graphics.fill(left + 1, top + 1, left + WIDTH - 1, top + 2, 0xFF8C55CC);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFE8D8FF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String label(ScryMarksPacket.Entry entry) {
        String prefix = "player".equals(entry.type()) ? "Entity" : "Location";
        return prefix + ": " + entry.name();
    }
}
