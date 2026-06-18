package com.ourmagic.client;

import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.TeleportPortalDataPacket;
import com.ourmagic.network.TeleportPortalUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public class TeleportPortalScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 238;
    private static final int ROW_H = 22;
    private static final int LIST_ROWS = 6;

    private final BlockPos source;
    private final String initialName;
    private final List<TeleportPortalDataPacket.Entry> entries;
    private EditBox nameBox;
    private Optional<BlockPos> selectedTarget;
    private int scrollOffset;

    public TeleportPortalScreen(TeleportPortalDataPacket packet) {
        super(Component.literal("Teleportation Portal"));
        this.source = packet.source();
        this.initialName = packet.name();
        this.entries = packet.entries();
        this.selectedTarget = packet.target();
    }

    public static void open(TeleportPortalDataPacket packet) {
        Minecraft.getInstance().setScreen(new TeleportPortalScreen(packet));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        nameBox = new EditBox(font, left + 18, top + 38, PANEL_W - 36, 20, Component.literal("Portal name"));
        nameBox.setMaxLength(40);
        nameBox.setValue(initialName);
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(left + PANEL_W - 176, top + PANEL_H - 32, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), button -> selectedTarget = Optional.empty())
                .bounds(left + PANEL_W - 94, top + PANEL_H - 32, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(left + 18, top + PANEL_H - 32, 76, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE100819);
        graphics.fill(left + 4, top + 4, left + PANEL_W - 4, top + PANEL_H - 4, 0xDD1B0E2A);
        graphics.hLine(left + 12, left + PANEL_W - 12, top + 27, 0xFF55E8FF);
        graphics.drawString(font, "Teleportation Portal", left + 18, top + 12, 0xFFEBD6FF, false);
        graphics.drawString(font, "Name", left + 18, top + 28, 0xFFBFA7D8, false);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderPortalList(graphics, left, top, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        int listX = left + 18;
        int listY = top + 72;
        int listW = PANEL_W - 36;
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + LIST_ROWS * ROW_H) {
            int index = scrollOffset + (((int) mouseY - listY) / ROW_H);
            if (index >= 0 && index < entries.size()) {
                selectedTarget = Optional.of(entries.get(index).pos());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, entries.size() - LIST_ROWS);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(delta)));
        return true;
    }

    private void renderPortalList(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int listX = left + 18;
        int listY = top + 72;
        int listW = PANEL_W - 36;
        graphics.drawString(font, "Destination", listX, top + 60, 0xFFBFA7D8, false);
        graphics.fill(listX - 2, listY - 2, listX + listW + 2, listY + LIST_ROWS * ROW_H + 2, 0xAA05030A);

        if (entries.isEmpty()) {
            graphics.drawString(font, "No other loaded portals", listX + 8, listY + 8, 0xFFFF7777, false);
            return;
        }

        for (int row = 0; row < LIST_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= entries.size()) {
                break;
            }
            TeleportPortalDataPacket.Entry entry = entries.get(index);
            int y = listY + row * ROW_H;
            boolean selected = selectedTarget.isPresent() && selectedTarget.get().equals(entry.pos());
            boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + ROW_H;
            if (selected || hovered) {
                graphics.fill(listX, y, listX + listW, y + ROW_H - 1, selected ? 0x8844BFE8 : 0x553A2358);
            }
            graphics.drawString(font, compact(entry.name(), 32), listX + 6, y + 3,
                    selected ? 0xFF55E8FF : 0xFFEBD6FF, false);
            graphics.drawString(font, entry.pos().toShortString(), listX + 6, y + 13, 0xFF8F7BA8, false);
        }
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new TeleportPortalUpdatePacket(source, nameBox.getValue(), selectedTarget));
    }

    private static String compact(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
