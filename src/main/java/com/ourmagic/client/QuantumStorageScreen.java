package com.ourmagic.client;

import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.QuantumStorageExtractPacket;
import com.ourmagic.network.QuantumStorageInsertPacket;
import com.ourmagic.network.QuantumStorageQueryPacket;
import com.ourmagic.ui.QuantumStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public class QuantumStorageScreen extends AbstractContainerScreen<QuantumStorageMenu> {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 320;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_COLS = 10;
    private static final int GRID_ROWS = 6;
    private EditBox searchBox;
    private int scrollOffset;
    private int refreshTicks;
    private SortMode sortMode = SortMode.NAME;
    private boolean draggingScrollbar;

    public QuantumStorageScreen(QuantumStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 79;
        inventoryLabelY = 220;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(font, leftPos + 16, topPos + 34, imageWidth - 32, 18, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search"));
        searchBox.setResponder(value -> scrollOffset = 0);
        addRenderableWidget(searchBox);
        addRenderableWidget(Button.builder(Component.literal("Insert Held"), button -> {
            ModNetwork.CHANNEL.sendToServer(new QuantumStorageInsertPacket(QuantumStorageInsertPacket.Mode.CARRIED, -1));
            requestRefresh();
        }).bounds(leftPos + 16, topPos + 200, 92, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Insert All"), button -> {
            ModNetwork.CHANNEL.sendToServer(new QuantumStorageInsertPacket(QuantumStorageInsertPacket.Mode.ALL, -1));
            requestRefresh();
        }).bounds(leftPos + 112, topPos + 200, 82, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Sort: Name"), button -> {
            sortMode = sortMode.next();
            button.setMessage(Component.literal("Sort: " + sortMode.label()));
            scrollOffset = 0;
        }).bounds(leftPos + 198, topPos + 200, 106, 16).build());
        requestRefresh();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE100819);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, 0xFF55E8FF);
        graphics.fill(leftPos + 14, topPos + 58, leftPos + imageWidth - 14, topPos + 194, 0xAA05030A);
        graphics.fill(leftPos + 75, topPos + 228, leftPos + 245, topPos + 310, 0xAA05030A);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = leftPos + 18 + col * 28;
                int y = topPos + 62 + row * 22;
                graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0xFF1D102A);
                drawBorder(graphics, x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, 0x664A2F68);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            requestRefresh();
        }
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderRows(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ClientQuantumStorageData.Summary summary = ClientQuantumStorageData.get(menu.corePos()).summary();
        graphics.drawString(font, "Quantum Storage", 16, 12, 0xFFEBD6FF, false);
        graphics.drawString(font, summary.bays() + " bays, " + summary.drives() + " drives, " + summary.used() + "/" + summary.capacity() + " items",
                142, 12, 0xFFBFA7D8, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbarContains(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        SlotHit hit = slotAt(mouseX, mouseY);
        if (hit != null) {
            List<ClientQuantumStorageData.Entry> entries = filteredEntries();
            int index = scrollOffset + hit.index();
            if (index >= 0 && index < entries.size()) {
                ClientQuantumStorageData.Entry entry = entries.get(index);
                if (hasShiftDown()) {
                    ModNetwork.CHANNEL.sendToServer(new QuantumStorageInsertPacket(QuantumStorageInsertPacket.Mode.MATCHING, entry.index()));
                } else {
                    ModNetwork.CHANNEL.sendToServer(new QuantumStorageExtractPacket(entry.index(), button == 0));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalRows = Math.max(0, (filteredEntries().size() + GRID_COLS - 1) / GRID_COLS);
        int maxRow = Math.max(0, totalRows - GRID_ROWS);
        int rowOffset = Math.max(0, Math.min(maxRow, scrollOffset / GRID_COLS - (int) Math.signum(delta)));
        scrollOffset = rowOffset * GRID_COLS;
        return true;
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ClientQuantumStorageData.Entry> entries = filteredEntries();
        for (int slot = 0; slot < visibleSlots(); slot++) {
            int index = scrollOffset + slot;
            if (index >= entries.size()) {
                break;
            }
            ClientQuantumStorageData.Entry entry = entries.get(index);
            int x = leftPos + 18 + (slot % GRID_COLS) * 28;
            int y = topPos + 62 + (slot / GRID_COLS) * 22;
            boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
            ItemStack icon = entry.stack();
            graphics.renderItem(icon, x + 1, y + 1);
            renderCompactCount(graphics, compactCount(entry.count()), x, y);
            if (hovered) {
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x55FFFFFF);
                graphics.renderTooltip(font, List.of(
                        icon.getHoverName(),
                        Component.literal(entry.count() + " stored")
                ), icon.getTooltipImage(), mouseX, mouseY);
            }
        }
        if (entries.isEmpty()) {
            graphics.drawString(font, "No stored items", leftPos + 22, topPos + 66, 0xFFFF7777, false);
        }
        renderScrollbar(graphics, entries.size());
        graphics.drawString(font, "Click: extract stack  Right-click: extract 1  Shift-click: insert matching", leftPos + 16, topPos + 188, 0xFF8F7BA8, false);
    }

    private List<ClientQuantumStorageData.Entry> filteredEntries() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<ClientQuantumStorageData.Entry> entries = sortEntries(ClientQuantumStorageData.get(menu.corePos()).entries());
        if (query.isEmpty()) {
            return entries;
        }
        return entries.stream()
                .filter(entry -> entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<ClientQuantumStorageData.Entry> sortEntries(List<ClientQuantumStorageData.Entry> entries) {
        java.util.ArrayList<ClientQuantumStorageData.Entry> sorted = new java.util.ArrayList<>(entries);
        sorted.sort((a, b) -> switch (sortMode) {
            case NAME -> a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
            case COUNT -> Integer.compare(b.count(), a.count());
            case ITEM_ID -> String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(a.stack().getItem()))
                    .compareToIgnoreCase(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(b.stack().getItem())));
        });
        return sorted;
    }

    private void requestRefresh() {
        ModNetwork.CHANNEL.sendToServer(new QuantumStorageQueryPacket(menu.corePos()));
    }

    private SlotHit slotAt(double mouseX, double mouseY) {
        int gridX = leftPos + 18;
        int gridY = topPos + 62;
        for (int slot = 0; slot < visibleSlots(); slot++) {
            int x = gridX + (slot % GRID_COLS) * 28;
            int y = gridY + (slot / GRID_COLS) * 22;
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                return new SlotHit(slot);
            }
        }
        return null;
    }

    private void renderScrollbar(GuiGraphics graphics, int totalEntries) {
        int x = leftPos + imageWidth - 18;
        int y = topPos + 62;
        int h = GRID_ROWS * 22 - 4;
        graphics.fill(x, y, x + 4, y + h, 0xFF1D102A);
        int totalRows = Math.max(0, (totalEntries + GRID_COLS - 1) / GRID_COLS);
        int maxRow = Math.max(0, totalRows - GRID_ROWS);
        int rowOffset = scrollOffset / GRID_COLS;
        int thumbH = maxRow == 0 ? h : Math.max(12, h * GRID_ROWS / Math.max(GRID_ROWS, totalRows));
        int thumbY = maxRow == 0 ? y : y + (h - thumbH) * rowOffset / maxRow;
        graphics.fill(x, thumbY, x + 4, thumbY + thumbH, 0xFF55E8FF);
    }

    private boolean scrollbarContains(double mouseX, double mouseY) {
        int x = leftPos + imageWidth - 18;
        int y = topPos + 62;
        int h = GRID_ROWS * 22 - 4;
        return mouseX >= x - 3 && mouseX < x + 7 && mouseY >= y && mouseY < y + h;
    }

    private void updateScrollFromMouse(double mouseY) {
        int totalEntries = filteredEntries().size();
        int totalRows = Math.max(0, (totalEntries + GRID_COLS - 1) / GRID_COLS);
        int maxRow = Math.max(0, totalRows - GRID_ROWS);
        if (maxRow == 0) {
            scrollOffset = 0;
            return;
        }
        int y = topPos + 62;
        int h = GRID_ROWS * 22 - 4;
        double percent = Math.max(0.0D, Math.min(1.0D, (mouseY - y) / h));
        scrollOffset = Math.max(0, Math.min(maxRow, (int) Math.round(percent * maxRow))) * GRID_COLS;
    }

    private static int visibleSlots() {
        return GRID_COLS * GRID_ROWS;
    }

    private static String compactCount(int count) {
        if (count >= 1_000_000_000) {
            return count / 1_000_000_000 + "B";
        }
        if (count >= 10_000_000) {
            return count / 1_000_000 + "M";
        }
        if (count >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%.1fM", count / 1_000_000.0F);
        }
        if (count >= 10_000) {
            return count / 1_000 + "K";
        }
        if (count >= 1_000) {
            return String.format(java.util.Locale.ROOT, "%.1fK", count / 1_000.0F);
        }
        return String.valueOf(count);
    }

    private void renderCompactCount(GuiGraphics graphics, String text, int slotX, int slotY) {
        int maxWidth = SLOT_SIZE - 2;
        int x = slotX + SLOT_SIZE - 1 - font.width(text);
        int y = slotY + SLOT_SIZE - 8;
        float scale = 1.0F;
        int width = font.width(text);
        if (width > maxWidth) {
            scale = maxWidth / (float) width;
            x = slotX + SLOT_SIZE - 1 - maxWidth;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 200.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private record SlotHit(int index) {
    }

    private enum SortMode {
        NAME("Name"),
        COUNT("Count"),
        ITEM_ID("ID");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }

        private SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
