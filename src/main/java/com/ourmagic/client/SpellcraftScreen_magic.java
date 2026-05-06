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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class SpellcraftScreen extends AbstractContainerScreen<SpellcraftMenu> {
    private static final String MOD_ID = "ourmagic";

    private static final int WIDTH = 360;
    private static final int HEIGHT = 286;

    private static final int EFFECT_X = 18;
    private static final int EFFECT_Y = 56;
    private static final int EFFECT_COLUMNS = 2;
    private static final int EFFECT_VISIBLE = 10;

    private static final int SHAPE_X = 148;
    private static final int SHAPE_Y = 56;
    private static final int SHAPE_VISIBLE = 5;

    private static final int EFFECT_BUTTON_W = 52;
    private static final int SHAPE_BUTTON_W = 70;
    private static final int BUTTON_H = 14;
    private static final int GAP = 4;
    private static final int ACTION_SIZE = 18;

    private static final int COLOR_CYAN = 0xFF55E8FF;
    private static final int COLOR_GREEN = 0xFF67FF61;
    private static final int COLOR_GOLD = 0xFFFFD84A;
    private static final int COLOR_PURPLE = 0xFFC66CFF;
    private static final int COLOR_RED = 0xFFFF6666;
    private static final int COLOR_SOFT_WHITE = 0xFFE9D8FF;

    private static final ResourceLocation BG = tex("bg.png");
    private static final ResourceLocation FRAME = tex("frame.png");
    private static final ResourceLocation PANEL = tex("panel.png");
    private static final ResourceLocation BOX = tex("box.png");
    private static final ResourceLocation BUTTON = tex("button.png");
    private static final ResourceLocation BUTTON_SELECTED = tex("button_selected.png");
    private static final ResourceLocation DIVIDER = tex("divider.png");
    private static final ResourceLocation SEPARATOR = tex("separator.png");
    private static final ResourceLocation RUNE_CIRCLE = tex("rune_circle.png");
    private static final ResourceLocation FEATHER = tex("feather.png");

    private static final ResourceLocation WAND_ICON = tex("icons/ui/wand.png");
    private static final ResourceLocation PAPER_ICON = tex("icons/ui/paper.png");
    private static final ResourceLocation GRIMOIRE_ICON = tex("icons/ui/grimoire.png");
    private static final ResourceLocation CLEAR_ICON = tex("icons/ui/clear.png");

    private final List<MagicButton> effectButtons = new ArrayList<>();
    private final List<MagicButton> shapeButtons = new ArrayList<>();
    private final List<String> selectedEffects = new ArrayList<>();

    private MagicButton clearButton;
    private MagicButton addToWandButton;
    private MagicButton paperButton;
    private MagicButton grimoireButton;

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
            MagicButton button = addRenderableWidget(new MagicButton(
                    leftPos + effectX(i), topPos + effectY(i), EFFECT_BUTTON_W, BUTTON_H,
                    b -> toggleEffect(effectScrollOffset + index)));
            effectButtons.add(button);
        }

        for (int i = 0; i < SHAPE_VISIBLE; i++) {
            int index = i;
            MagicButton button = addRenderableWidget(new MagicButton(
                    leftPos + SHAPE_X, topPos + SHAPE_Y + i * (BUTTON_H + GAP), SHAPE_BUTTON_W, BUTTON_H,
                    b -> selectedShape = shapeScrollOffset + index));
            shapeButtons.add(button);
        }

        addToWandButton = addRenderableWidget(new MagicButton(leftPos + 323, topPos + 55, ACTION_SIZE, ACTION_SIZE, b -> craft(CraftSpellPacket.Target.WAND)));
        addToWandButton.setIcon(WAND_ICON);
        addToWandButton.setTooltip(Tooltip.create(Component.literal("Add to wand")));

        paperButton = addRenderableWidget(new MagicButton(leftPos + 323, topPos + 78, ACTION_SIZE, ACTION_SIZE, b -> craft(CraftSpellPacket.Target.PAPER)));
        paperButton.setIcon(PAPER_ICON);
        paperButton.setTooltip(Tooltip.create(Component.literal("Make spell paper")));

        grimoireButton = addRenderableWidget(new MagicButton(leftPos + 323, topPos + 101, ACTION_SIZE, ACTION_SIZE, b -> craft(CraftSpellPacket.Target.GRIMOIRE)));
        grimoireButton.setIcon(GRIMOIRE_ICON);
        grimoireButton.setTooltip(Tooltip.create(Component.literal("Add to grimoire")));

        clearButton = addRenderableWidget(new MagicButton(leftPos + 323, topPos + 122, ACTION_SIZE, ACTION_SIZE, b -> selectedEffects.clear()));
        clearButton.setIcon(CLEAR_ICON);
        clearButton.setTooltip(Tooltip.create(Component.literal("Clear selection")));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        blit(graphics, BG, leftPos, topPos, WIDTH, HEIGHT, 512, 512);
        blit(graphics, FRAME, leftPos, topPos, WIDTH, HEIGHT, WIDTH, HEIGHT);

        blit(graphics, PANEL, leftPos + 12, topPos + 32, imageWidth - 24, 119, 64, 64);
        blit(graphics, PANEL, leftPos + 12, topPos + 183, imageWidth - 24, imageHeight - 191, 64, 64);

        drawTextureBox(graphics, 14, 47, 116, 96);
        drawTextureBox(graphics, 142, 47, 84, 96);
        drawTextureBox(graphics, 238, 47, 78, 96);
        drawTextureBox(graphics, 16, 190, 74, 78);
        drawTextureBox(graphics, 94, 190, 250, 78);

        blit(graphics, DIVIDER, leftPos + 133, topPos + 42, 3, 102, 3, 128);
        blit(graphics, DIVIDER, leftPos + 229, topPos + 42, 3, 102, 3, 128);
        blit(graphics, SEPARATOR, leftPos + 279, topPos + 55, 5, 77, 5, 128);

        blit(graphics, RUNE_CIRCLE, leftPos + 250, topPos + 55, 54, 54, 96, 96);
        blit(graphics, RUNE_CIRCLE, leftPos + 157, topPos + 144, 58, 58, 96, 96);
        blit(graphics, FEATHER, leftPos + 248, topPos + 204, 44, 44, 64, 64);
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

        graphics.drawCenteredString(font, "✦  SPELLCRAFT  ✦", imageWidth / 2, 13, COLOR_SOFT_WHITE);
        graphics.drawString(font, "1. Choose Effects", 16, 37, COLOR_CYAN, false);
        graphics.drawString(font, "2. Choose Shape", 146, 37, COLOR_GREEN, false);
        graphics.drawString(font, "3. Your Spell", 242, 37, COLOR_GOLD, false);
        graphics.drawString(font, "Ingredients", 19, 176, COLOR_GOLD, false);
        graphics.drawString(font, "Inventory", 99, 174, COLOR_SOFT_WHITE, false);

        renderEffectScroll(graphics, effects.size());
        renderShapeScroll(graphics, shapes.size());
        renderSelectedRecipe(graphics);
        renderActionLabels(graphics);
    }

    private void updateButtons(List<String> effects, List<String> shapes) {
        for (int i = 0; i < effectButtons.size(); i++) {
            MagicButton button = effectButtons.get(i);
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
            button.setWidth(EFFECT_BUTTON_W);
            button.setMessage(Component.literal(shortLabel(effect, 6)).withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            button.setSelected(selected);
            button.setIcon(effectIcon(effect));
            button.setTooltip(Tooltip.create(Component.literal(titleCase(effect))));
        }

        for (int i = 0; i < shapeButtons.size(); i++) {
            MagicButton button = shapeButtons.get(i);
            int shapeIndex = shapeScrollOffset + i;
            if (shapeIndex >= shapes.size()) {
                button.visible = false;
                continue;
            }

            String shape = shapes.get(shapeIndex);
            boolean selected = shapeIndex == selectedShape;
            button.visible = true;
            button.active = true;
            button.setX(leftPos + SHAPE_X);
            button.setY(topPos + SHAPE_Y + i * (BUTTON_H + GAP));
            button.setWidth(SHAPE_BUTTON_W);
            button.setMessage(Component.literal(shortLabel(shape, 8)).withStyle(selected ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            button.setSelected(selected);
            button.setIcon(shapeIcon(shape));
            button.setTooltip(Tooltip.create(Component.literal(titleCase(shape))));
        }
    }

    private void renderSelectedRecipe(GuiGraphics graphics) {
        String key = selectedSpellKey();
        Spell spell = SpellRegistry.get(key);
        boolean craftable = spell != null && !selectedEffects.isEmpty();

        graphics.drawCenteredString(font, compact(key, 12), 277, 113, craftable ? COLOR_GOLD : COLOR_RED);
        graphics.drawString(font, selectedEffects.size() + "/" + maxEffects() + " effects", 242, 128, COLOR_CYAN, false);

        renderSelectedIcons(graphics);

        if (spell != null) {
            graphics.drawString(font, "Mana  " + spell.manaCost(), 242, 143, COLOR_CYAN, false);
            graphics.drawString(font, "Delay " + String.format("%.1fs", spell.cooldownTicks() / 20.0F), 242, 155, COLOR_GOLD, false);
            String status = hasIngredients(key) ? "Ready" : isUnlocked(key) ? "Known" : "Needs items";
            graphics.drawString(font, compact(status, 12), 242, 167, isUnlocked(key) ? COLOR_GREEN : COLOR_RED, false);
        } else {
            graphics.drawString(font, "No recipe", 242, 154, COLOR_RED, false);
        }

        renderRequirements(graphics, key);
        boolean canCraft = craftable && isUnlocked(key);
        addToWandButton.active = canCraft;
        paperButton.active = canCraft;
        grimoireButton.active = canCraft;
    }

    private void renderSelectedIcons(GuiGraphics graphics) {
        int centerX = 277;
        int centerY = 82;
        int[][] positions = {{centerX - 22, centerY}, {centerX, centerY}, {centerX + 22, centerY}};

        for (int i = 0; i < Math.min(3, selectedEffects.size()); i++) {
            ResourceLocation icon = effectIcon(selectedEffects.get(i));
            if (icon != null) {
                blit(graphics, icon, positions[i][0] - 8, positions[i][1] - 8, 16, 16, 24, 24);
            }
        }
    }

    private void renderActionLabels(GuiGraphics graphics) {
        graphics.drawString(font, "Add", 344, 57, COLOR_SOFT_WHITE, false);
        graphics.drawString(font, "Paper", 344, 80, COLOR_SOFT_WHITE, false);
        graphics.drawString(font, "Book", 344, 103, COLOR_SOFT_WHITE, false);
        graphics.drawString(font, "Clear", 344, 125, COLOR_SOFT_WHITE, false);
    }

    private void toggleEffect(int index) {
        List<String> effects = craftableEffects();
        if (index < 0 || index >= effects.size()) return;

        String effect = effects.get(index);
        if (selectedEffects.contains(effect)) {
            if (selectedEffects.size() > 1) selectedEffects.remove(effect);
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
        if (selectedEffects.isEmpty() || shapes.isEmpty()) return "";
        return String.join("+", selectedEffects) + "@" + shapes.get(selectedShape);
    }

    private List<String> craftableEffects() {
        return SpellRegistry.payloadKeys().stream()
                .filter(effect -> !SpellRegistry.allowedShapesForPayloads(List.of(effect)).isEmpty())
                .filter(effect -> hasPayloadIngredients(effect) || hasGrimoirePayload(effect))
                .toList();
    }

    private List<String> craftableShapes(List<String> effects) {
        if (selectedEffects.isEmpty() && !effects.isEmpty()) {
            return SpellRegistry.allowedShapesForPayloads(List.of(effects.get(0)));
        }
        return SpellRegistry.allowedShapesForPayloads(selectedEffects).stream()
                .filter(shape -> !selectedEffects.isEmpty())
                .toList();
    }

    private boolean hasIngredients(String key) {
        return minecraft != null && minecraft.player != null && SpellIngredients.has(minecraft.player.getInventory(), key);
    }

    private boolean isUnlocked(String key) {
        return minecraft != null && minecraft.player != null && SpellIngredients.isUnlocked(minecraft.player.getInventory(), key);
    }

    private boolean hasPayloadIngredients(String payload) {
        return minecraft != null && minecraft.player != null && SpellIngredients.hasPayloadRequirements(minecraft.player.getInventory(), payload + "@self");
    }

    private boolean hasGrimoirePayload(String payload) {
        return minecraft != null && minecraft.player != null && SpellIngredients.grimoirePayloads(minecraft.player.getInventory()).contains(payload);
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

    private void drawTextureBox(GuiGraphics graphics, int x, int y, int width, int height) {
        blit(graphics, BOX, leftPos + x, topPos + y, width, height, 64, 64);
    }

    private static int effectX(int index) {
        return EFFECT_X + (index % EFFECT_COLUMNS) * (EFFECT_BUTTON_W + GAP);
    }

    private static int effectY(int index) {
        return EFFECT_Y + (index / EFFECT_COLUMNS) * (BUTTON_H + GAP);
    }

    private static ResourceLocation tex(String path) {
        return new ResourceLocation(MOD_ID, "textures/gui/spellcraft/" + path);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
    }

    private static ResourceLocation effectIcon(String effect) {
        String key = normalize(effect);
        return switch (key) {
            case "fire", "frost", "heal", "wither", "lightning", "levitate", "shield", "regrowth",
                 "blink", "water_bolt", "reveal", "sunburst", "meteor", "flame_wave", "thorns", "void_pulse" ->
                    tex("icons/effects/" + key + ".png");
            default -> null;
        };
    }

    private static ResourceLocation shapeIcon(String shape) {
        String key = normalize(shape);
        return switch (key) {
            case "projectile", "nova", "beam", "cone", "burst" -> tex("icons/shapes/" + key + ".png");
            default -> null;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace(' ', '_').replace('-', '_');
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

    private class MagicButton extends Button {
        private boolean selected;
        private ResourceLocation icon;

        MagicButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        void setIcon(ResourceLocation icon) {
            this.icon = icon;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture = selected || isHoveredOrFocused() ? BUTTON_SELECTED : BUTTON;
            blit(graphics, texture, getX(), getY(), width, height, 96, 24);

            if (!active) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88000000);
            }

            if (icon != null) {
                int iconSize = Math.min(12, height - 2);
                blit(graphics, icon, getX() + 3, getY() + (height - iconSize) / 2, iconSize, iconSize, 24, 24);
            }

            String text = getMessage().getString();
            if (!text.isEmpty()) {
                int color = active ? (selected ? COLOR_SOFT_WHITE : 0xFFC6B6D8) : 0xFF5F5868;
                int textX = icon == null ? getX() + 5 : getX() + 18;
                graphics.drawString(font, compact(text, icon == null ? 8 : 6), textX, getY() + 3, color, false);
            }
        }
    }
}
