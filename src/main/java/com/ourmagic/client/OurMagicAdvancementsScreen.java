package com.ourmagic.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ourmagic.OurMagic;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OurMagicAdvancementsScreen extends Screen {
    private static final int NODE_SIZE = 26;
    private static final int NODE_HALF = NODE_SIZE / 2;
    private static final int PANEL = 30;
    private static final int COLOR_BG = 0xF00B0812;
    private static final int COLOR_PANEL = 0xCC161020;
    private static final int COLOR_GRID = 0x223C2A4A;
    private static final int COLOR_TEXT = 0xFFEBD6FF;
    private static final int COLOR_MUTED = 0xFF9D8FB1;
    private static final double MIN_ZOOM = 0.45D;
    private static final double MAX_ZOOM = 2.25D;
    private static final double ZOOM_STEP = 1.12D;

    private final ClientAdvancements clientAdvancements;
    private final List<Node> nodes = new ArrayList<>();
    private final Map<ResourceLocation, Node> nodesById = new HashMap<>();
    private Node selected;
    private double panX;
    private double panY;
    private double zoom = 1.0D;
    private boolean dragging;

    public OurMagicAdvancementsScreen(ClientAdvancements clientAdvancements) {
        super(Component.literal("OurMagic Advancements"));
        this.clientAdvancements = clientAdvancements;
    }

    @Override
    protected void init() {
        reloadNodes();
        centerOnRoot();
        addRenderableWidget(Button.builder(Component.literal("Vanilla"), button -> OurMagicAdvancementScreenEvents.openVanilla(clientAdvancements))
                .bounds(width - 86, 8, 72, 20)
                .build());
    }

    private void reloadNodes() {
        nodes.clear();
        nodesById.clear();
        Map<ResourceLocation, BetterDisplay> betterDisplays = loadBetterDisplays();
        Map<Advancement, AdvancementProgress> progress = readProgress(clientAdvancements);

        for (Advancement advancement : clientAdvancements.getAdvancements().getAllAdvancements()) {
            ResourceLocation id = advancement.getId();
            if (!OurMagic.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith("spells/") || advancement.getDisplay() == null) {
                continue;
            }

            BetterDisplay display = betterDisplays.getOrDefault(id, BetterDisplay.fromAdvancement(advancement));
            AdvancementProgress advancementProgress = progress.get(advancement);
            Node node = new Node(advancement, display, advancementProgress != null && advancementProgress.isDone(),
                    advancementProgress != null && advancementProgress.hasProgress());
            nodes.add(node);
            nodesById.put(id, node);
        }

        nodes.sort(Comparator.comparingInt((Node node) -> node.display.y()).thenComparingInt(node -> node.display.x()));
        selected = nodes.stream()
                .filter(node -> node.id().getPath().equals("spells/root"))
                .findFirst()
                .orElse(nodes.isEmpty() ? null : nodes.get(0));
    }

    private Map<ResourceLocation, BetterDisplay> loadBetterDisplays() {
        Map<ResourceLocation, BetterDisplay> displays = new HashMap<>();
        Minecraft minecraft = Minecraft.getInstance();
        Map<ResourceLocation, Resource> resources = minecraft.getResourceManager().listResources("advancements",
                id -> OurMagic.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("advancements/spells/") && id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (!json.has("better_display") || !json.get("better_display").isJsonObject()) {
                    continue;
                }

                String path = entry.getKey().getPath();
                path = path.substring("advancements/".length(), path.length() - ".json".length());
                displays.put(new ResourceLocation(entry.getKey().getNamespace(), path), BetterDisplay.fromJson(json.getAsJsonObject("better_display")));
            } catch (Exception ex) {
                OurMagic.LOGGER.warn("Could not load OurMagic advancement display metadata for {}", entry.getKey(), ex);
            }
        }

        return displays;
    }

    @SuppressWarnings("unchecked")
    private static Map<Advancement, AdvancementProgress> readProgress(ClientAdvancements advancements) {
        for (String fieldName : List.of("progress", "f_104390_")) {
            try {
                Field field = ClientAdvancements.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (Map<Advancement, AdvancementProgress>) field.get(advancements);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return Map.of();
    }

    private void centerOnRoot() {
        Node root = nodesById.get(new ResourceLocation(OurMagic.MOD_ID, "spells/root"));
        if (root == null) {
            panX = PANEL + 40;
            panY = height / 2.0;
            return;
        }
        panX = width * 0.18 - root.display.x();
        panY = height * 0.50 - root.display.y();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, COLOR_BG);
        drawGrid(graphics);

        graphics.pose().pushPose();
        graphics.pose().translate(panX, panY, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1.0F);
        drawConnections(graphics);
        drawNodes(graphics, screenToWorldX(mouseX), screenToWorldY(mouseY));
        graphics.pose().popPose();

        drawHeader(graphics);
        drawDetails(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawGrid(GuiGraphics graphics) {
        int spacing = Math.max(12, (int) Math.round(48 * zoom));
        int offsetX = Math.floorMod((int) Math.round(panX), spacing);
        int offsetY = Math.floorMod((int) Math.round(panY), spacing);
        for (int x = offsetX; x < width; x += spacing) {
            graphics.fill(x, 0, x + 1, height, COLOR_GRID);
        }
        for (int y = offsetY; y < height; y += spacing) {
            graphics.fill(0, y, width, y + 1, COLOR_GRID);
        }
    }

    private void drawConnections(GuiGraphics graphics) {
        for (Node node : nodes) {
            Advancement parent = node.advancement.getParent();
            if (parent == null) {
                continue;
            }
            Node parentNode = nodesById.get(parent.getId());
            if (parentNode == null) {
                continue;
            }
            int color = node.done ? node.display.completedLineColor() : node.display.uncompletedLineColor();
            drawLine(graphics, parentNode.display.x(), parentNode.display.y(), node.display.x(), node.display.y(), color);
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        if (Math.abs(x2 - x1) >= Math.abs(y2 - y1)) {
            int midX = (x1 + x2) / 2;
            graphics.fill(minX, y1, maxX + 1, y1 + 2, color);
            graphics.fill(midX, minY, midX + 2, maxY + 1, color);
            graphics.fill(midX, y2, maxX + 1, y2 + 2, color);
        } else {
            graphics.fill(x1, minY, x1 + 2, maxY + 1, color);
            graphics.fill(minX, y2, maxX + 1, y2 + 2, color);
        }
    }

    private void drawNodes(GuiGraphics graphics, double mouseWorldX, double mouseWorldY) {
        Node hovered = nodeAt(mouseWorldX, mouseWorldY);
        for (Node node : nodes) {
            boolean isSelected = node == selected;
            boolean isHovered = node == hovered;
            int x = node.display.x() - NODE_HALF;
            int y = node.display.y() - NODE_HALF;
            int frame = node.done ? node.display.completedIconColor() : node.display.uncompletedIconColor();
            int fill = node.done ? 0xEE241A2D : node.started ? 0xDD181625 : 0xCC0E0D14;

            graphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, isSelected ? 0xFFFFD86A : isHovered ? 0xFFFFFFFF : 0xAA000000);
            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, fill);
            drawBorder(graphics, x, y, NODE_SIZE, NODE_SIZE, frame);
            ItemStack icon = node.advancement.getDisplay().getIcon();
            graphics.renderItem(icon, x + 5, y + 5);
            graphics.drawString(font, node.advancement.getDisplay().getTitle(), x + NODE_SIZE + 6, y + 8,
                    node.done ? node.display.completedTitleColor() : node.display.uncompletedTitleColor(), false);
        }
    }

    private void drawHeader(GuiGraphics graphics) {
        graphics.fill(0, 0, width, PANEL, COLOR_PANEL);
        graphics.drawString(font, "OurMagic Advancements", 12, 11, COLOR_TEXT, false);
        graphics.drawString(font, "Drag to pan. Mouse wheel zooms.", 154, 11, COLOR_MUTED, false);
        graphics.drawString(font, String.format("Zoom %.0f%%", zoom * 100.0D), width - 160, 11, COLOR_MUTED, false);
    }

    private void drawDetails(GuiGraphics graphics) {
        int panelWidth = Math.min(320, width - 24);
        int x = 12;
        int y = height - 92;
        graphics.fill(x, y, x + panelWidth, height - 12, COLOR_PANEL);
        drawBorder(graphics, x, y, panelWidth, 80, 0xFF6F4A96);
        if (selected == null) {
            graphics.drawString(font, "No OurMagic advancements loaded.", x + 12, y + 12, COLOR_TEXT, false);
            return;
        }

        graphics.renderItem(selected.advancement.getDisplay().getIcon(), x + 12, y + 12);
        graphics.drawString(font, selected.advancement.getDisplay().getTitle(), x + 36, y + 12,
                selected.done ? selected.display.completedTitleColor() : selected.display.uncompletedTitleColor(), false);
        graphics.drawString(font, selected.done ? "Complete" : selected.started ? "In progress" : "Locked", x + 36, y + 25,
                selected.done ? 0xFF79FF8B : selected.started ? 0xFFFFD86A : COLOR_MUTED, false);
        graphics.drawWordWrap(font, selected.advancement.getDisplay().getDescription(), x + 12, y + 44, panelWidth - 24, COLOR_TEXT);
    }

    private Node nodeAt(double worldX, double worldY) {
        for (Node node : nodes) {
            int x = node.display.x() - NODE_HALF;
            int y = node.display.y() - NODE_HALF;
            if (worldX >= x - 2 && worldX < x + NODE_SIZE + 2 && worldY >= y - 2 && worldY < y + NODE_SIZE + 2) {
                return node;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        Node clicked = nodeAt(screenToWorldX(mouseX), screenToWorldY(mouseY));
        if (clicked != null) {
            selected = clicked;
            return true;
        }
        if (button == 0) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (super.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }

        double oldZoom = zoom;
        double direction = Math.signum(delta);
        if (direction == 0.0D) {
            return false;
        }

        double worldX = screenToWorldX(mouseX);
        double worldY = screenToWorldY(mouseY);
        zoom = Mth.clamp(direction > 0.0D ? zoom * ZOOM_STEP : zoom / ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        if (zoom == oldZoom) {
            return true;
        }

        panX = mouseX - worldX * zoom;
        panY = mouseY - worldY * zoom;
        return true;
    }

    private double screenToWorldX(double screenX) {
        return (screenX - panX) / zoom;
    }

    private double screenToWorldY(double screenY) {
        return (screenY - panY) / zoom;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static int color(JsonObject json, String key, int fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        String value = json.get(key).getAsString().replace("#", "");
        try {
            return 0xFF000000 | Integer.parseInt(value, 16);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private record Node(Advancement advancement, BetterDisplay display, boolean done, boolean started) {
        ResourceLocation id() {
            return advancement.getId();
        }
    }

    private record BetterDisplay(
            int x,
            int y,
            int completedIconColor,
            int uncompletedIconColor,
            int completedTitleColor,
            int uncompletedTitleColor,
            int completedLineColor,
            int uncompletedLineColor
    ) {
        static BetterDisplay fromJson(JsonObject json) {
            return new BetterDisplay(
                    json.has("pos_x") ? json.get("pos_x").getAsInt() : 0,
                    json.has("pos_y") ? json.get("pos_y").getAsInt() : 0,
                    color(json, "completed_icon_color", 0xFFFFD86A),
                    color(json, "uncompleted_icon_color", 0xFF67517A),
                    color(json, "completed_title_color", 0xFFFFFFFF),
                    color(json, "uncompleted_title_color", 0xFFB9A6D6),
                    color(json, "completed_line_color", 0xFFC66CFF),
                    color(json, "uncompleted_line_color", 0xFF5B4566)
            );
        }

        static BetterDisplay fromAdvancement(Advancement advancement) {
            int x = advancement.getDisplay() == null ? 0 : Math.round(advancement.getDisplay().getX() * 32.0F);
            int y = advancement.getDisplay() == null ? 0 : Math.round(advancement.getDisplay().getY() * 27.0F);
            return new BetterDisplay(x, y, 0xFFFFD86A, 0xFF67517A, 0xFFFFFFFF, 0xFFB9A6D6, 0xFFC66CFF, 0xFF5B4566);
        }
    }
}
