package com.ourmagic.client;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellIngredients;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.network.CraftSpellPacket;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.ui.SpellcraftMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class SpellcraftScreen extends AbstractContainerScreen<SpellcraftMenu> {
    private static final int WIDTH = 360;
    private static final int HEIGHT = 286;
    private static final int EFFECT_X = 16;
    private static final int EFFECT_Y = 55;
    private static final int EFFECT_COLUMNS = 2;
    private static final int EFFECT_VISIBLE = 10;
    private static final int SHAPE_X = 146;
    private static final int SHAPE_Y = 55;
    private static final int SHAPE_VISIBLE = 5;
    private static final int BUTTON_W = 52;
    private static final int BUTTON_H = 14;
    private static final int GAP = 4;
    private static final int ACTION_SIZE = 18;
    private static final int COLOR_FRAME = 0xFF6F4A96;
    private static final int COLOR_PANEL = 0xAA080A12;
    private static final int COLOR_BOX = 0xBB060812;
    private static final int COLOR_LINE = 0x884A5268;
    private static final int COLOR_CYAN = 0xFF55E8FF;
    private static final int COLOR_GREEN = 0xFF67FF61;
    private static final int COLOR_GOLD = 0xFFFFD84A;
    private static final int COLOR_PURPLE = 0xFFC66CFF;

    private final List<Button> effectButtons = new ArrayList<>();
    private final List<Button> shapeButtons = new ArrayList<>();
    private final List<String> selectedEffects = new ArrayList<>();
    private Button clearButton;
    private Button addToWandButton;
    private Button paperButton;
    private Button grimoireButton;
    private int effectScrollOffset;
    private int shapeScrollOffset;
    private int selectedShape;

    public SpellcraftScreen(SpellcraftMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = HEIGHT + 100;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        effectButtons.clear();
        shapeButtons.clear();

        for (int i = 0; i < EFFECT_VISIBLE; i++) {
            int index = i;
            Button button = addRenderableWidget(Button.builder(Component.empty(), b -> toggleEffect(effectScrollOffset + index))
                    .bounds(leftPos + effectX(i), topPos + effectY(i), BUTTON_W, BUTTON_H)
                    .build());
            effectButtons.add(button);
        }

        for (int i = 0; i < SHAPE_VISIBLE; i++) {
            int index = i;
            Button button = addRenderableWidget(Button.builder(Component.empty(), b -> selectedShape = shapeScrollOffset + index)
                    .bounds(leftPos + SHAPE_X, topPos + SHAPE_Y + i * (BUTTON_H + GAP), 70, BUTTON_H)
                    .build());
            shapeButtons.add(button);
        }

        clearButton = addRenderableWidget(Button.builder(yellow("X"), b -> selectedEffects.clear())
                .bounds(leftPos + 323, topPos + 122, ACTION_SIZE, ACTION_SIZE)
                .tooltip(Tooltip.create(Component.literal("Clear selection")))
                .build());
        addToWandButton = addRenderableWidget(Button.builder(yellow("W"), b -> craft(CraftSpellPacket.Target.WAND))
                .bounds(leftPos + 323, topPos + 55, ACTION_SIZE, ACTION_SIZE)
                .tooltip(Tooltip.create(Component.literal("Add to wand")))
                .build());
        paperButton = addRenderableWidget(Button.builder(yellow("P"), b -> craft(CraftSpellPacket.Target.PAPER))
                .bounds(leftPos + 323, topPos + 78, ACTION_SIZE, ACTION_SIZE)
                .tooltip(Tooltip.create(Component.literal("Make spell paper")))
                .build());
        grimoireButton = addRenderableWidget(Button.builder(yellow("G"), b -> craft(CraftSpellPacket.Target.GRIMOIRE))
                .bounds(leftPos + 323, topPos + 101, ACTION_SIZE, ACTION_SIZE)
                .tooltip(Tooltip.create(Component.literal("Add to grimoire")))
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE17141F);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, COLOR_FRAME);
        graphics.fill(leftPos + 12, topPos + 32, leftPos + imageWidth - 12, topPos + 151, COLOR_PANEL);
        graphics.fill(leftPos + 12, topPos + 183, leftPos + imageWidth - 12, topPos + imageHeight - 8, COLOR_PANEL);
        drawBox(graphics, leftPos + 14, topPos + 47, 116, 96);
        drawBox(graphics, leftPos + 142, topPos + 47, 84, 96);
        drawBox(graphics, leftPos + 238, topPos + 47, 78, 96);
        drawBox(graphics, leftPos + 16, topPos + 190, 74, 78);
        graphics.fill(leftPos + 134, topPos + 42, leftPos + 135, topPos + 144, COLOR_LINE);
        graphics.fill(leftPos + 230, topPos + 42, leftPos + 231, topPos + 144, COLOR_LINE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + 14 && mouseX < leftPos + 130 && mouseY >= topPos + 47 && mouseY < topPos + 143) {
            effectScrollOffset -= (int) Math.signum(delta);
            clampScrolls();
            return true;
        }
        if (mouseX >= leftPos + 142 && mouseX < leftPos + 226 && mouseY >= topPos + 47 && mouseY < topPos + 143) {
            shapeScrollOffset -= (int) Math.signum(delta);
            clampScrolls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> effects = craftableEffects();
        List<String> shapes = craftableShapes(effects);
        selectedEffects.removeIf(effect -> !effects.contains(effect));
        if (selectedEffects.isEmpty() && !effects.isEmpty()) {
            selectedEffects.add(effects.get(0));
        }
        selectedShape = Math.max(0, Math.min(selectedShape, Math.max(0, shapes.size() - 1)));
        clampScrolls();
        updateButtons(effects, shapes);

        graphics.drawString(font, "Spellcraft", 14, 13, 0xFFFFFFFF, false);
        graphics.drawCenteredString(font, "*", imageWidth / 2, 17, COLOR_PURPLE);
        graphics.drawString(font, "Effects", 16, 37, COLOR_CYAN, false);
        graphics.drawString(font, "Shape", 146, 37, COLOR_GREEN, false);
        graphics.drawString(font, "Output", 242, 37, COLOR_GOLD, false);
        graphics.drawString(font, "Reqs", 19, 176, COLOR_GOLD, false);
        graphics.drawString(font, "Inventory", 99, 174, 0xFFFFFFFF, false);

        renderEffectScroll(graphics, effects.size());
        renderShapeScroll(graphics, shapes.size());
        renderSelectedRecipe(graphics);
    }

    private void updateButtons(List<String> effects, List<String> shapes) {
        for (int i = 0; i < effectButtons.size(); i++) {
            Button button = effectButtons.get(i);
            int effectIndex = effectScrollOffset + i;
            if (effectIndex >= effects.size()) {
                button.visible = false;
                continue;
            }
            String effect = effects.get(effectIndex);
            boolean selected = selectedEffects.contains(effect);
            button.visible = true;
            button.active = true;
            button.setX(leftPos + effectX(i));
            button.setY(topPos + effectY(i));
            button.setWidth(BUTTON_W);
            button.setMessage(Component.literal(shortLabel(effect, 6)).withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            button.setTooltip(Tooltip.create(Component.literal(titleCase(effect))));
        }

        for (int i = 0; i < shapeButtons.size(); i++) {
            Button button = shapeButtons.get(i);
            int shapeIndex = shapeScrollOffset + i;
            if (shapeIndex >= shapes.size()) {
                button.visible = false;
                continue;
            }
            String shape = shapes.get(shapeIndex);
            button.visible = true;
            button.active = true;
            button.setX(leftPos + SHAPE_X);
            button.setY(topPos + SHAPE_Y + i * (BUTTON_H + GAP));
            button.setWidth(66);
            button.setMessage(Component.literal(shortLabel(shape, 8)).withStyle(shapeIndex == selectedShape ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            button.setTooltip(Tooltip.create(Component.literal(titleCase(shape))));
        }
    }

    private void renderSelectedRecipe(GuiGraphics graphics) {
        String key = selectedSpellKey();
        Spell spell = SpellRegistry.get(key);
        boolean craftable = spell != null && !selectedEffects.isEmpty();
        graphics.drawString(font, compact(key, 10), 242, 56, craftable ? COLOR_GOLD : 0xFFFF6666, false);
        graphics.drawString(font, selectedEffects.size() + "/" + maxEffects(), 242, 70, COLOR_CYAN, false);
        if (spell != null) {
            graphics.drawString(font, "M " + spell.manaCost(), 242, 84, COLOR_CYAN, false);
            graphics.drawString(font, String.format("%.1fs", spell.cooldownTicks() / 20.0F), 242, 98, COLOR_GOLD, false);
            String status = hasIngredients(key) ? "Ready" : isUnlocked(key) ? "Known" : "Needs items";
            graphics.drawString(font, compact(status, 10), 242, 112, isUnlocked(key) ? COLOR_GREEN : 0xFFFF6666, false);
        } else {
            graphics.drawString(font, "No recipe", 242, 112, 0xFFFF6666, false);
        }
        renderRequirements(graphics, key);
        boolean canCraft = craftable && isUnlocked(key);
        addToWandButton.active = canCraft;
        paperButton.active = canCraft;
        grimoireButton.active = canCraft;
    }

    private void toggleEffect(int index) {
        List<String> effects = craftableEffects();
        if (index < 0 || index >= effects.size()) {
            return;
        }
        String effect = effects.get(index);
        if (selectedEffects.contains(effect)) {
            if (selectedEffects.size() > 1) {
                selectedEffects.remove(effect);
            }
            return;
        }
        if (maxEffects() == 1) {
            selectedEffects.clear();
            selectedEffects.add(effect);
            selectedShape = 0;
            shapeScrollOffset = 0;
            return;
        }
        if (selectedEffects.size() < maxEffects()) {
            selectedEffects.add(effect);
        }
    }

    private void craft(CraftSpellPacket.Target target) {
        String key = selectedSpellKey();
        if (SpellRegistry.get(key) != null && !selectedEffects.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new CraftSpellPacket(menu.hand(), key, target));
        }
    }

    private String selectedSpellKey() {
        List<String> shapes = craftableShapes(craftableEffects());
        if (selectedEffects.isEmpty() || shapes.isEmpty()) {
            return "";
        }
        return String.join("+", selectedEffects) + "@" + shapes.get(selectedShape);
    }

    private List<String> craftableEffects() {
        return SpellRegistry.payloadKeys().stream()
                .filter(effect -> SpellRegistry.shapeKeys().stream().anyMatch(shape -> SpellRegistry.isValidRecipe(effect + "@" + shape)))
                .filter(effect -> isUnlocked(effect + "@self"))
                .toList();
    }

    private List<String> craftableShapes(List<String> effects) {
        if (selectedEffects.isEmpty() && !effects.isEmpty()) {
            return SpellRegistry.shapeKeys();
        }
        return SpellRegistry.shapeKeys().stream()
                .filter(shape -> !selectedEffects.isEmpty()
                        && SpellRegistry.isValidRecipe(String.join("+", selectedEffects) + "@" + shape)
                        && isUnlocked(String.join("+", selectedEffects) + "@" + shape))
                .toList();
    }

    private boolean hasIngredients(String key) {
        return minecraft != null && minecraft.player != null && SpellIngredients.has(minecraft.player.getInventory(), key);
    }

    private boolean isUnlocked(String key) {
        return minecraft != null && minecraft.player != null && SpellIngredients.isUnlocked(minecraft.player.getInventory(), key);
    }

    private void renderRequirements(GuiGraphics graphics, String key) {
        if (!hasIngredients(key) && isUnlocked(key)) {
            graphics.drawString(font, "Grimoire", 21, 200, 0xFFB8FFB8, false);
            return;
        }

        List<SpellIngredients.Requirement> requirements = SpellIngredients.requirementsFor(key);
        int x = 21;
        int y = 198;
        for (int i = 0; i < Math.min(5, requirements.size()); i++) {
            SpellIngredients.Requirement requirement = requirements.get(i);
            int amount = minecraft == null || minecraft.player == null ? 0 : SpellIngredients.count(minecraft.player.getInventory(), requirement);
            String line = compact(requirement.displayName().getString(), 6) + " " + amount + "/" + requirement.count();
            graphics.drawString(font, line, x, y + i * 10, amount >= requirement.count() ? 0xFFB8FFB8 : 0xFFFF7777, false);
        }
    }

    private int maxEffects() {
        int highestLevel = menu.wandData().spells().stream().mapToInt(spell -> spell.level()).max().orElse(1);
        return highestLevel >= 50 ? 3 : highestLevel >= 20 ? 2 : 1;
    }

    private void clampScrolls() {
        effectScrollOffset = Math.max(0, Math.min(effectScrollOffset, Math.max(0, craftableEffects().size() - EFFECT_VISIBLE)));
        shapeScrollOffset = Math.max(0, Math.min(shapeScrollOffset, Math.max(0, craftableShapes(craftableEffects()).size() - SHAPE_VISIBLE)));
    }

    private void renderEffectScroll(GuiGraphics graphics, int total) {
        renderScroll(graphics, 124, EFFECT_Y, 3, 84, effectScrollOffset, total, EFFECT_VISIBLE);
    }

    private void renderShapeScroll(GuiGraphics graphics, int total) {
        renderScroll(graphics, 219, SHAPE_Y, 3, 84, shapeScrollOffset, total, SHAPE_VISIBLE);
    }

    private void renderScroll(GuiGraphics graphics, int x, int y, int width, int height, int offset, int total, int visible) {
        if (total <= visible) return;
        graphics.fill(x, y, x + width, y + height, 0x552A3040);
        int thumbHeight = Math.max(12, height * visible / total);
        int maxOffset = Math.max(1, total - visible);
        int thumbY = y + (height - thumbHeight) * offset / maxOffset;
        graphics.fill(x, thumbY, x + width, thumbY + thumbHeight, COLOR_PURPLE);
    }

    private static int effectX(int index) {
        return EFFECT_X + (index % EFFECT_COLUMNS) * (BUTTON_W + GAP);
    }

    private static int effectY(int index) {
        return EFFECT_Y + (index / EFFECT_COLUMNS) * (BUTTON_H + GAP);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawBox(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLOR_BOX);
        drawBorder(graphics, x, y, width, height, 0x66343A4F);
    }

    private static Component yellow(String text) {
        return Component.literal(text).withStyle(ChatFormatting.YELLOW);
    }

    private static String shortLabel(String value, int max) {
        return compact(value, max);
    }

    private static String compact(String value, int max) {
        String label = titleCase(value);
        return label.length() <= max ? label : label.substring(0, max);
    }

    private static String titleCase(String value) {
        if (value == null || value.isEmpty()) return "None";
        String cleaned = value.replace("_", " ");
        return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }
}
