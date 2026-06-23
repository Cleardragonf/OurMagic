package com.ourmagic.client;

import com.ourmagic.magic.SpellIngredients;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.network.CraftWardPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.ui.WardCrafterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WardCrafterScreen extends AbstractContainerScreen<WardCrafterMenu> {
    private static final int WIDTH = 360;
    private static final int HEIGHT = 416;
    private static final int VISIBLE_PAYLOADS = 6;
    private static final int VISIBLE_TARGETS = 6;
    private static final int ROW_H = 18;
    private static final int COLOR_TEXT = 0xFFEBD6FF;
    private static final int COLOR_MUTED = 0xFFBFA7D8;
    private static final int COLOR_CYAN = 0xFF55E8FF;
    private static final int COLOR_GOLD = 0xFFFFD66B;

    private static final List<TargetOption> TARGETS = List.of(
            new TargetOption("Non Allied", "ward_non_allied", false, ""),
            new TargetOption("Hostile", "ward_hostile", false, ""),
            new TargetOption("Players", "ward_players", false, ""),
            new TargetOption("Allies", "ward_allies", false, ""),
            new TargetOption("Mobs", "ward_mobs", false, ""),
            new TargetOption("Monsters", "ward_monsters", false, ""),
            new TargetOption("Passive", "ward_passive", false, ""),
            new TargetOption("Animals", "ward_animals", false, ""),
            new TargetOption("Any", "ward_any", false, ""),
            new TargetOption("Player Name", "ward_player_", true, "Player name"),
            new TargetOption("Entity Type", "ward_entity_type_", true, "minecraft:zombie")
    );

    private final List<Button> payloadButtons = new ArrayList<>();
    private final List<Button> targetButtons = new ArrayList<>();
    private final List<String> selectedPayloads = new ArrayList<>();
    private EditBox targetParameter;
    private Button diagramButton;
    private Button grimoireButton;
    private Button clearButton;
    private int payloadScroll;
    private int targetScroll;
    private int selectedTarget;

    public WardCrafterScreen(WardCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 99;
        inventoryLabelY = 312;
    }

    @Override
    protected void init() {
        super.init();
        payloadButtons.clear();
        targetButtons.clear();

        for (int i = 0; i < VISIBLE_PAYLOADS; i++) {
            int index = i;
            payloadButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> togglePayload(payloadScroll + index))
                    .bounds(leftPos + 24, topPos + 48 + i * ROW_H, 132, 16)
                    .build()));
        }
        for (int i = 0; i < VISIBLE_TARGETS; i++) {
            int index = i;
            targetButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> selectedTarget = targetScroll + index)
                    .bounds(leftPos + 204, topPos + 48 + i * ROW_H, 132, 16)
                    .build()));
        }

        targetParameter = addRenderableWidget(new EditBox(font, leftPos + 204, topPos + 160, 132, 18, Component.literal("Target value")));
        targetParameter.setMaxLength(48);
        targetParameter.setHint(Component.literal("target"));

        diagramButton = addRenderableWidget(Button.builder(Component.literal("Make Diagram"), button -> craft(CraftWardPacket.Target.DIAGRAM))
                .bounds(leftPos + 72, topPos + 294, 104, 18)
                .build());
        grimoireButton = addRenderableWidget(Button.builder(Component.literal("Add Grimoire"), button -> craft(CraftWardPacket.Target.GRIMOIRE))
                .bounds(leftPos + 184, topPos + 294, 104, 18)
                .build());
        clearButton = addRenderableWidget(Button.builder(Component.literal("Clear"), button -> selectedPayloads.clear())
                .bounds(leftPos + 24, topPos + 160, 132, 16)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE100819);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, 0xFF55E8FF);
        panel(graphics, 16, 34, 152, 166);
        panel(graphics, 192, 34, 152, 166);
        panel(graphics, 48, 216, 264, 70);
        panel(graphics, 92, 322, 176, 80);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0x66190725);
            drawBorder(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0x6655E8FF);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderRequirementTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> payloads = craftablePayloads();
        selectedPayloads.removeIf(payload -> !payloads.contains(payload));
        selectedTarget = Math.max(0, Math.min(selectedTarget, TARGETS.size() - 1));
        clampScrolls(payloads);
        updateButtons(payloads);

        graphics.drawCenteredString(font, "WARD CRAFTER", imageWidth / 2, 12, COLOR_TEXT);
        graphics.drawString(font, "1. Payloads", 24, 28, COLOR_CYAN, false);
        graphics.drawString(font, "2. Target", 204, 28, COLOR_GOLD, false);
        graphics.drawString(font, "Requirements", 56, 208, COLOR_MUTED, false);
        graphics.drawString(font, "Inventory", inventoryLabelX, inventoryLabelY, COLOR_MUTED, false);

        String key = recipeKey();
        if (key.isEmpty()) {
            graphics.drawCenteredString(font, "Select payloads and a target", imageWidth / 2, 207, COLOR_MUTED);
        } else {
            graphics.drawCenteredString(font, compact(key, 46), imageWidth / 2, 207, COLOR_TEXT);
        }
        renderRequirements(graphics, key);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + 16 && mouseX < leftPos + 168 && mouseY >= topPos + 34 && mouseY < topPos + 200) {
            payloadScroll -= (int) Math.signum(delta);
            clampScrolls(craftablePayloads());
            return true;
        }
        if (mouseX >= leftPos + 192 && mouseX < leftPos + 344 && mouseY >= topPos + 34 && mouseY < topPos + 200) {
            targetScroll -= (int) Math.signum(delta);
            clampScrolls(craftablePayloads());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void updateButtons(List<String> payloads) {
        for (int i = 0; i < payloadButtons.size(); i++) {
            Button button = payloadButtons.get(i);
            int payloadIndex = payloadScroll + i;
            if (payloadIndex >= payloads.size()) {
                button.visible = false;
                continue;
            }
            String payload = payloads.get(payloadIndex);
            boolean selected = selectedPayloads.contains(payload);
            boolean canSelect = selected || canAddPayload(payload);
            button.visible = true;
            button.active = canSelect;
            button.setMessage(Component.literal((selected ? "* " : "") + compact(titleCase(payload), 17))
                    .withStyle(selected ? ChatFormatting.AQUA : canSelect ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
        }

        for (int i = 0; i < targetButtons.size(); i++) {
            Button button = targetButtons.get(i);
            int targetIndex = targetScroll + i;
            if (targetIndex >= TARGETS.size()) {
                button.visible = false;
                continue;
            }
            TargetOption target = TARGETS.get(targetIndex);
            boolean selected = selectedTarget == targetIndex;
            button.visible = true;
            button.active = true;
            button.setMessage(Component.literal((selected ? "* " : "") + target.label())
                    .withStyle(selected ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        }

        TargetOption target = TARGETS.get(selectedTarget);
        targetParameter.visible = target.requiresParameter();
        targetParameter.setHint(Component.literal(target.hint()));
        String key = recipeKey();
        boolean valid = !key.isEmpty() && hasIngredients(key);
        diagramButton.active = valid;
        grimoireButton.active = valid;
        clearButton.active = !selectedPayloads.isEmpty();
    }

    private void togglePayload(int index) {
        List<String> payloads = craftablePayloads();
        if (index < 0 || index >= payloads.size()) {
            return;
        }
        String payload = payloads.get(index);
        if (selectedPayloads.remove(payload)) {
            return;
        }
        if (canAddPayload(payload)) {
            selectedPayloads.add(payload);
        }
    }

    private boolean canAddPayload(String payload) {
        if (selectedPayloads.size() >= 2 || selectedPayloads.contains(payload)) {
            return false;
        }
        List<String> payloads = new ArrayList<>();
        payloads.add("ward");
        payloads.addAll(selectedPayloads);
        payloads.add(payload);
        return !SpellRegistry.allowedShapesForPayloads(payloads).stream().filter(shape -> shape.startsWith("ward_")).toList().isEmpty();
    }

    private void craft(CraftWardPacket.Target target) {
        String key = recipeKey();
        if (!key.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new CraftWardPacket(key, target));
        }
    }

    private String recipeKey() {
        if (selectedPayloads.isEmpty()) {
            return "";
        }
        TargetOption target = TARGETS.get(selectedTarget);
        String shape = target.shapeKey();
        if (target.requiresParameter()) {
            String parameter = normalizeParameter(targetParameter.getValue());
            if (parameter.isEmpty()) {
                return "";
            }
            shape += parameter;
        }
        List<String> payloads = new ArrayList<>();
        payloads.add("ward");
        payloads.addAll(selectedPayloads);
        String key = String.join("+", payloads) + "@" + shape;
        return SpellRegistry.isValidRecipe(key) ? key : "";
    }

    private List<String> craftablePayloads() {
        return SpellRegistry.payloadKeys().stream()
                .filter(payload -> !payload.equals("ward"))
                .filter(payload -> !SpellRegistry.allowedShapesForPayloads(List.of("ward", payload)).isEmpty())
                .filter(payload -> hasAnyPayloadIngredient(payload) || hasKnownPayload(payload))
                .toList();
    }

    private boolean hasAnyPayloadIngredient(String payload) {
        return minecraft != null && minecraft.player != null
                && SpellIngredients.hasAnyPayloadRequirement(minecraft.player.getInventory(), payload);
    }

    private boolean hasKnownPayload(String payload) {
        return minecraft != null && minecraft.player != null
                && SpellIngredients.hasArcaneKnowledgePayload(minecraft.player.getInventory(), payload);
    }

    private boolean hasIngredients(String key) {
        return minecraft != null && minecraft.player != null && SpellIngredients.has(minecraft.player.getInventory(), key);
    }

    private void renderRequirements(GuiGraphics graphics, String key) {
        if (key.isEmpty()) {
            return;
        }
        List<SpellIngredients.Requirement> requirements = SpellIngredients.requirementsFor(key);
        int x = 58;
        int y = 224;
        int columns = 4;
        for (int i = 0; i < requirements.size(); i++) {
            SpellIngredients.Requirement requirement = requirements.get(i);
            ItemStack display = requirement.display().copy();
            int amount = minecraft == null || minecraft.player == null ? 0 : SpellIngredients.count(minecraft.player.getInventory(), requirement);
            int iconX = x + (i % columns) * 62;
            int iconY = y + (i / columns) * 21;
            graphics.fill(iconX - 2, iconY - 2, iconX + 18, iconY + 18, 0xAA080411);
            drawBorder(graphics, iconX - 2, iconY - 2, 20, 20, amount >= requirement.count() ? 0xAA55E8FF : 0xAAFF6666);
            graphics.renderItem(display, iconX, iconY);
            graphics.drawString(font, amount + "/" + requirement.count(), iconX + 20, iconY + 5,
                    amount >= requirement.count() ? 0xFFB8FFB8 : 0xFFFF7777, false);
        }
    }

    private void renderRequirementTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        String key = recipeKey();
        if (key.isEmpty()) {
            return;
        }
        List<SpellIngredients.Requirement> requirements = SpellIngredients.requirementsFor(key);
        int x = leftPos + 58;
        int y = topPos + 224;
        int columns = 4;
        for (int i = 0; i < requirements.size(); i++) {
            int iconX = x + (i % columns) * 62;
            int iconY = y + (i / columns) * 21;
            if (mouseX >= iconX - 2 && mouseX < iconX + 18 && mouseY >= iconY - 2 && mouseY < iconY + 18) {
                SpellIngredients.Requirement requirement = requirements.get(i);
                int amount = minecraft == null || minecraft.player == null ? 0 : SpellIngredients.count(minecraft.player.getInventory(), requirement);
                graphics.renderTooltip(font, requirement.displayName().copy().append(" " + amount + "/" + requirement.count()), mouseX, mouseY);
                return;
            }
        }
    }

    private void clampScrolls(List<String> payloads) {
        payloadScroll = Math.max(0, Math.min(payloadScroll, Math.max(0, payloads.size() - VISIBLE_PAYLOADS)));
        targetScroll = Math.max(0, Math.min(targetScroll, Math.max(0, TARGETS.size() - VISIBLE_TARGETS)));
    }

    private void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xAA05030A);
        drawBorder(graphics, leftPos + x, topPos + y, width, height, 0x664A2F68);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static String normalizeParameter(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_:\\-/.]", "");
    }

    private static String compact(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + ".";
    }

    private static String titleCase(String value) {
        if (value == null || value.isEmpty()) {
            return "None";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : value.replace('_', ' ').split(" ")) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return builder.toString();
    }

    private record TargetOption(String label, String shapeKey, boolean requiresParameter, String hint) {
    }
}
