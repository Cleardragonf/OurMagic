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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpellcraftScreen extends AbstractContainerScreen<SpellcraftMenu> {
    private static final String MOD_ID = "ourmagic";
    private static final int WIDTH = 512;
    private static final int HEIGHT = 410;
    private static final int EFFECT_X = 30;
    private static final int EFFECT_Y = 94;
    private static final int EFFECT_COLUMNS = 1;
    private static final int EFFECT_VISIBLE = 4;
    private static final int SHAPE_X = 174;
    private static final int SHAPE_Y = 94;
    private static final int SHAPE_VISIBLE = 4;
    private static final int SHAPE_BUTTON_W = 124;
    private static final int BUTTON_W = 112;
    private static final int BUTTON_H = 34;
    private static final int GAP = 7;
    private static final int ACTION_SIZE = 28;
    private static final int LOWER_OFFSET = 44;
    private static final int EFFECT_PANEL_X = 18;
    private static final int EFFECT_PANEL_Y = 61;
    private static final int EFFECT_PANEL_W = 142;
    private static final int EFFECT_PANEL_H = 206;
    private static final int SHAPE_PANEL_X = 170;
    private static final int SHAPE_PANEL_Y = 61;
    private static final int SHAPE_PANEL_W = 144;
    private static final int SHAPE_PANEL_H = 206;
    private static final int SPELL_PANEL_X = 324;
    private static final int SPELL_PANEL_Y = 61;
    private static final int SPELL_PANEL_W = 162;
    private static final int SPELL_PANEL_H = 206;
    private static final int LOWER_PANEL_X = 112;
    private static final int LOWER_PANEL_Y = 292;
    private static final int LOWER_PANEL_W = 336;
    private static final int LOWER_PANEL_H = 95;
    private static final int COLOR_FRAME = 0xFFB85CFF;
    private static final int COLOR_FRAME_DARK = 0xFF3A2358;
    private static final int COLOR_PANEL = 0xDD070711;
    private static final int COLOR_BOX = 0xD9040610;
    private static final int COLOR_LINE = 0xAA5B3C78;
    private static final int COLOR_CYAN = 0xFF55E8FF;
    private static final int COLOR_GREEN = 0xFF67FF61;
    private static final int COLOR_GOLD = 0xFFFFD84A;
    private static final int COLOR_PURPLE = 0xFFC66CFF;
    private static final int COLOR_TEXT = 0xFFEBD6FF;

    private static final ResourceLocation FOREGROUND_FRAME = tex("foreground_frame.png");
    private static final ResourceLocation SPELLCRAFT_ATLAS = tex("spellcraft_atlas.png");
    private static final ResourceLocation SHAPE_ICONS_ATLAS = tex("icons/shapes/shape_icons_atlas.png");
    private static final ResourceLocation BUTTON_NORMAL = tex("button_normal.png");
    private static final ResourceLocation BUTTON_SELECTED = tex("button_selected.png");
    private static final ResourceLocation BUTTON_DISABLED = tex("button_disabled.png");
    private static final ResourceLocation BUTTON_SHAPE_NORMAL = tex("button_shape_normal.png");
    private static final ResourceLocation BUTTON_SHAPE_SELECTED = tex("button_shape_selected.png");
    private static final ResourceLocation BUTTON_SHAPE_DISABLED = tex("button_shape_disabled.png");
    private static final ResourceLocation ACTION_WAND = tex("action_wand.png");
    private static final ResourceLocation ACTION_PAPER = tex("action_paper.png");
    private static final ResourceLocation ACTION_GRIMOIRE = tex("action_grimoire.png");
    private static final ResourceLocation ACTION_CLEAR = tex("action_clear.png");
    private static final int ACTION_WAND_U = 696;
    private static final int ACTION_PAPER_U = 824;
    private static final int ACTION_GRIMOIRE_U = 952;
    private static final int ACTION_CLEAR_U = 1104;
    private static final int ACTION_SELECTED_V = 535;
    private static final int ACTION_AVAILABLE_V = 647;
    private static final int ACTION_DISABLED_V = 756;
    private static final int ACTION_SOURCE_SIZE = 96;
    private static final int SCROLL_ACTIVE_U = 1383;
    private static final int SCROLL_ACTIVE_V = 25;
    private static final int SCROLL_ACTIVE_W = 40;
    private static final int SCROLL_ACTIVE_H = 524;
    private static final int SCROLL_INACTIVE_U = 1454;
    private static final int SCROLL_INACTIVE_V = 30;
    private static final int SCROLL_INACTIVE_W = 41;
    private static final int SCROLL_INACTIVE_H = 518;
    private static final int FOREGROUND_PANEL_U = 696;
    private static final int FOREGROUND_PANEL_V = 0;
    private static final int FOREGROUND_PANEL_W = 320;
    private static final int FOREGROUND_PANEL_H = 350;
    private static final int LOWER_PANEL_U = 28;
    private static final int LOWER_PANEL_V = 820;
    private static final int LOWER_PANEL_SOURCE_W = 628;
    private static final int LOWER_PANEL_SOURCE_H = 127;
    private static final int SLOT_U = 28;
    private static final int SLOT_V = 578;
    private static final int SLOT_W = 72;
    private static final int SLOT_H = 70;

    private final List<MagicButton> effectButtons = new ArrayList<>();
    private final List<MagicButton> shapeButtons = new ArrayList<>();
    private final List<RequirementIcon> requirementIcons = new ArrayList<>();
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
            MagicButton button = addRenderableWidget(new MagicButton(leftPos + effectX(i), topPos + effectY(i), BUTTON_W, BUTTON_H,
                    b -> toggleEffect(effectScrollOffset + index)));
            effectButtons.add(button);
        }

        for (int i = 0; i < SHAPE_VISIBLE; i++) {
            int index = i;
            MagicButton button = addRenderableWidget(new MagicButton(leftPos + SHAPE_X, topPos + SHAPE_Y + i * (BUTTON_H + GAP), 124, BUTTON_H,
                    b -> selectedShape = shapeScrollOffset + index));
            shapeButtons.add(button);
        }

        addToWandButton = actionButton(438, 88, "W", "Add to wand", b -> craft(CraftSpellPacket.Target.WAND));
        addToWandButton.setActionSprite(ACTION_WAND_U);
        paperButton = actionButton(438, 124, "P", "Make spell paper", b -> craft(CraftSpellPacket.Target.PAPER));
        paperButton.setActionSprite(ACTION_PAPER_U);
        grimoireButton = actionButton(438, 160, "G", "Add to grimoire", b -> craft(CraftSpellPacket.Target.GRIMOIRE));
        grimoireButton.setActionSprite(ACTION_GRIMOIRE_U);
        clearButton = actionButton(438, 196, "X", "Clear selection", b -> selectedEffects.clear());
        clearButton.setActionSprite(ACTION_CLEAR_U);
    }

    private MagicButton actionButton(int x, int y, String label, String tooltip, Button.OnPress onPress) {
        MagicButton button = addRenderableWidget(new MagicButton(leftPos + x, topPos + y, ACTION_SIZE, ACTION_SIZE, onPress));
        button.setMessage(Component.literal(label).withStyle(ChatFormatting.GOLD));
        button.setGoldFrame(true);
        button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        return button;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(FOREGROUND_FRAME, leftPos, topPos, WIDTH, HEIGHT, 0, 0, 360, 286, 360, 286);
        graphics.blit(SPELLCRAFT_ATLAS, leftPos + EFFECT_PANEL_X, topPos + EFFECT_PANEL_Y, EFFECT_PANEL_W, EFFECT_PANEL_H,
                FOREGROUND_PANEL_U, FOREGROUND_PANEL_V, FOREGROUND_PANEL_W, FOREGROUND_PANEL_H, 1536, 1024);
        graphics.blit(SPELLCRAFT_ATLAS, leftPos + SHAPE_PANEL_X, topPos + SHAPE_PANEL_Y, SHAPE_PANEL_W, SHAPE_PANEL_H,
                FOREGROUND_PANEL_U, FOREGROUND_PANEL_V, FOREGROUND_PANEL_W, FOREGROUND_PANEL_H, 1536, 1024);
        graphics.blit(SPELLCRAFT_ATLAS, leftPos + SPELL_PANEL_X, topPos + SPELL_PANEL_Y, SPELL_PANEL_W, SPELL_PANEL_H,
                FOREGROUND_PANEL_U, FOREGROUND_PANEL_V, FOREGROUND_PANEL_W, FOREGROUND_PANEL_H, 1536, 1024);
        graphics.blit(SPELLCRAFT_ATLAS, leftPos + LOWER_PANEL_X, topPos + LOWER_PANEL_Y, LOWER_PANEL_W, LOWER_PANEL_H,
                LOWER_PANEL_U, LOWER_PANEL_V, LOWER_PANEL_SOURCE_W, LOWER_PANEL_SOURCE_H, 1536, 1024);
        renderInventorySlotFrames(graphics);
    }

    private void renderInventorySlotFrames(GuiGraphics graphics) {
        for (Slot slot : menu.slots) {
            graphics.blit(SPELLCRAFT_ATLAS, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18,
                    SLOT_U, SLOT_V, SLOT_W, SLOT_H, 1536, 1024);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + EFFECT_PANEL_X && mouseX < leftPos + EFFECT_PANEL_X + EFFECT_PANEL_W
                && mouseY >= topPos + EFFECT_PANEL_Y && mouseY < topPos + EFFECT_PANEL_Y + EFFECT_PANEL_H) {
            effectScrollOffset -= (int) Math.signum(delta);
            clampScrolls();
            return true;
        }
        if (mouseX >= leftPos + SHAPE_PANEL_X && mouseX < leftPos + SHAPE_PANEL_X + SHAPE_PANEL_W
                && mouseY >= topPos + SHAPE_PANEL_Y && mouseY < topPos + SHAPE_PANEL_Y + SHAPE_PANEL_H) {
            shapeScrollOffset -= (int) Math.signum(delta);
            clampScrolls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> effects = craftableEffects();
        selectedEffects.removeIf(effect -> !effects.contains(effect));
        if (selectedEffects.isEmpty() && !effects.isEmpty()) {
            selectedEffects.add(effects.get(0));
        }
        List<String> shapes = craftableShapes(effects);
        selectedShape = Math.max(0, Math.min(selectedShape, Math.max(0, shapes.size() - 1)));
        clampScrolls();
        updateButtons(effects, shapes);

        graphics.drawCenteredString(font, "SPELLCRAFT", imageWidth / 2, 13, COLOR_TEXT);
        graphics.drawString(font, "1. Choose Effects", 28, 52, COLOR_CYAN, false);
        graphics.drawString(font, "2. Choose Shape", 178, 52, COLOR_GREEN, false);
        graphics.drawString(font, "3. Your Spell", 318, 52, COLOR_GOLD, false);
        graphics.drawString(font, "Inventory", LOWER_PANEL_X + 12, LOWER_PANEL_Y + 4, COLOR_TEXT, false);

        renderEffectScroll(graphics, effects.size());
        renderShapeScroll(graphics, shapes.size());
        renderSelectedRecipe(graphics);
        renderLowerHint(graphics);
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
            boolean canSelect = selected || canAddEffect(effect);
            button.visible = true;
            button.active = canSelect;
            button.setX(leftPos + effectX(i));
            button.setY(topPos + effectY(i));
            button.setWidth(BUTTON_W);
            button.setMessage(Component.literal(shortLabel(effect, 6)).withStyle(selected ? ChatFormatting.AQUA : canSelect ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
            button.setSelected(selected);
            button.setIcon(effectIcon(effect));
            button.setTooltip(Tooltip.create(Component.literal(canSelect ? titleCase(effect) : titleCase(effect) + " cannot combine here")));
        }

        for (int i = 0; i < shapeButtons.size(); i++) {
            MagicButton button = shapeButtons.get(i);
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
            button.setWidth(SHAPE_BUTTON_W);
            boolean selected = shapeIndex == selectedShape;
            button.setMessage(Component.literal(shortLabel(shape, 8)).withStyle(selected ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            button.setSelected(selected);
            button.setShapeIcon(shape);
            button.setTooltip(Tooltip.create(Component.literal(titleCase(shape))));
        }
    }

    private void renderSelectedRecipe(GuiGraphics graphics) {
        String key = selectedSpellKey();
        Spell spell = SpellRegistry.get(key);
        boolean craftable = spell != null && !selectedEffects.isEmpty();
        renderSpellPreview(graphics, key, craftable);
        if (spell != null) {
            String status = hasIngredients(key) ? "Ready" : isUnlocked(key) ? "Known" : "Needs items";
            graphics.drawString(font, compact(status, 12), 334, 162, isUnlocked(key) ? COLOR_GREEN : 0xFFFF6666, false);
        } else {
            graphics.drawString(font, "No recipe", 334, 162, 0xFFFF6666, false);
        }
        renderRequirements(graphics, key);
        boolean canCraft = craftable && isUnlocked(key);
        addToWandButton.active = canCraft;
        paperButton.active = canCraft;
        grimoireButton.active = canCraft;
        addToWandButton.setTooltip(Tooltip.create(Component.literal("Add to wand")));
        paperButton.setTooltip(Tooltip.create(Component.literal("Make spell paper")));
        grimoireButton.setTooltip(Tooltip.create(Component.literal("Add to grimoire")));
    }

    private void renderSpellPreview(GuiGraphics graphics, String key, boolean craftable) {
        int centerY = 112;
        int effectX = 338;
        int shapeX = 382;

        if (selectedEffects.isEmpty()) {
            graphics.drawCenteredString(font, "Select", 360, centerY - 5, COLOR_TEXT);
            return;
        }

        int effectCount = Math.min(3, selectedEffects.size());
        int startX = effectX - (effectCount - 1) * 10;
        for (int i = 0; i < effectCount; i++) {
            blit(graphics, effectIcon(selectedEffects.get(i)), startX + i * 20, centerY - 8, 16, 16, 16, 16);
        }

        String shape = SpellRegistry.shapeKey(key);
        if (!shape.isEmpty()) {
            blitShapeIcon(graphics, shape, shapeX, centerY - 8, 16);
        }

        graphics.drawString(font, compact(key, 10), 334, 140, craftable ? COLOR_GOLD : 0xFFFF6666, false);
        graphics.drawString(font, previewDescription(key), 334, 156, COLOR_TEXT, false);
    }

    private static String previewDescription(String key) {
        String payload = titleCase(SpellRegistry.payloadKey(key));
        String shape = titleCase(SpellRegistry.shapeKey(key));
        if (payload.length() > 9) {
            payload = payload.substring(0, 9);
        }
        if (shape.length() > 9) {
            shape = shape.substring(0, 9);
        }
        return payload + " via " + shape;
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
        if (selectedEffects.size() < maxEffects() && canAddEffect(effect)) {
            selectedEffects.add(effect);
        }
    }

    private boolean canAddEffect(String effect) {
        if (selectedEffects.contains(effect)) {
            return true;
        }
        if (maxEffects() == 1) {
            return true;
        }
        if (selectedEffects.size() >= maxEffects()) {
            return false;
        }
        List<String> candidate = new ArrayList<>(selectedEffects);
        candidate.add(effect);
        return !SpellRegistry.allowedShapesForPayloads(candidate).isEmpty();
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
                .filter(effect -> !effect.equals("ward"))
                .filter(effect -> !SpellRegistry.allowedShapesForPayloads(List.of(effect)).isEmpty())
                .filter(effect -> hasPayloadIngredients(effect) || hasKnownPayload(effect))
                .toList();
    }

    private List<String> craftableShapes(List<String> effects) {
        if (selectedEffects.isEmpty() && !effects.isEmpty()) {
            return SpellRegistry.allowedShapesForPayloads(List.of(effects.get(0))).stream()
                    .filter(shape -> canUseShape(List.of(effects.get(0)), shape))
                    .toList();
        }
        return SpellRegistry.allowedShapesForPayloads(selectedEffects).stream()
                .filter(shape -> !selectedEffects.isEmpty())
                .filter(shape -> canUseShape(selectedEffects, shape))
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

    private boolean hasKnownPayload(String payload) {
        return minecraft != null && minecraft.player != null && SpellIngredients.knownPayloads(minecraft.player.getInventory()).contains(payload);
    }

    private boolean canUseShape(List<String> payloads, String shape) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        String key = String.join("+", payloads) + "@" + shape;
        return SpellIngredients.hasKnownSpell(minecraft.player.getInventory(), key)
                || SpellIngredients.hasAnyShapeRequirement(minecraft.player.getInventory(), shape);
    }

    private void renderRequirements(GuiGraphics graphics, String key) {
        requirementIcons.clear();
        if (!hasIngredients(key) && isUnlocked(key)) {
            graphics.drawString(font, "Known", 30, LOWER_PANEL_Y + 18, 0xFFB8FFB8, false);
            return;
        }

        List<SpellIngredients.Requirement> requirements = SpellIngredients.requirementsFor(key);
        int x = 30;
        int y = LOWER_PANEL_Y - 2;
        for (int i = 0; i < Math.min(5, requirements.size()); i++) {
            SpellIngredients.Requirement requirement = requirements.get(i);
            int amount = minecraft == null || minecraft.player == null ? 0 : SpellIngredients.count(minecraft.player.getInventory(), requirement);
            int iconY = y + i * 18;
            ItemStack display = requirement.display().copy();
            graphics.renderItem(display, x, iconY);
            String count = amount + "/" + requirement.count();
            graphics.drawString(font, count, x + 20, iconY + 5, amount >= requirement.count() ? 0xFFB8FFB8 : 0xFFFF7777, false);
            requirementIcons.add(new RequirementIcon(x, iconY, requirement, amount));
        }
    }

    private void renderLowerHint(GuiGraphics graphics) {
        int centerX = LOWER_PANEL_X + 252;
        int y = LOWER_PANEL_Y + 38;
        graphics.drawCenteredString(font, "Select effects and a shape", centerX, y, COLOR_TEXT);
        graphics.drawCenteredString(font, "to craft your spell.", centerX, y + 12, COLOR_TEXT);
    }

    private void renderRequirementTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (RequirementIcon icon : requirementIcons) {
            int x = leftPos + icon.x();
            int y = topPos + icon.y();
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                Component tooltip = icon.requirement().displayName().copy()
                        .append(Component.literal(" " + icon.amount() + "/" + icon.requirement().count()));
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    private int maxEffects() {
        return 3;
    }

    private void clampScrolls() {
        effectScrollOffset = Math.max(0, Math.min(effectScrollOffset, Math.max(0, craftableEffects().size() - EFFECT_VISIBLE)));
        shapeScrollOffset = Math.max(0, Math.min(shapeScrollOffset, Math.max(0, craftableShapes(craftableEffects()).size() - SHAPE_VISIBLE)));
    }

    private void renderEffectScroll(GuiGraphics graphics, int total) {
        renderScroll(graphics, 140, 90, 12, 164, effectScrollOffset, total, EFFECT_VISIBLE);
    }

    private void renderShapeScroll(GuiGraphics graphics, int total) {
        renderScroll(graphics, 300, 90, 12, 164, shapeScrollOffset, total, SHAPE_VISIBLE);
    }

    private void renderScroll(GuiGraphics graphics, int x, int y, int width, int height, int offset, int total, int visible) {
        boolean active = total > visible;
        int u = active ? SCROLL_ACTIVE_U : SCROLL_INACTIVE_U;
        int v = active ? SCROLL_ACTIVE_V : SCROLL_INACTIVE_V;
        int sourceWidth = active ? SCROLL_ACTIVE_W : SCROLL_INACTIVE_W;
        int sourceHeight = active ? SCROLL_ACTIVE_H : SCROLL_INACTIVE_H;
        if (!active) {
            graphics.blit(SPELLCRAFT_ATLAS, x, y, width, height, u, v, sourceWidth, sourceHeight, 1536, 1024);
            return;
        }

        int capHeight = Math.min(18, height / 3);
        int sourceCapHeight = Math.max(1, SCROLL_ACTIVE_H * capHeight / height);
        graphics.blit(SPELLCRAFT_ATLAS, x, y, width, capHeight, u, v, sourceWidth, sourceCapHeight, 1536, 1024);
        graphics.blit(SPELLCRAFT_ATLAS, x, y + height - capHeight, width, capHeight,
                u, v + SCROLL_ACTIVE_H - sourceCapHeight, sourceWidth, sourceCapHeight, 1536, 1024);

        int scrollAreaY = y + capHeight;
        int scrollAreaHeight = height - capHeight * 2;
        int sourceScrollY = v + sourceCapHeight;
        int sourceScrollHeight = SCROLL_ACTIVE_H - sourceCapHeight * 2;
        graphics.blit(SPELLCRAFT_ATLAS, x, scrollAreaY, width, scrollAreaHeight,
                u, sourceScrollY, sourceWidth, sourceScrollHeight, 1536, 1024);

        int thumbSourceHeight = Math.min(120, sourceScrollHeight);
        int thumbHeight = Math.max(22, Math.min(scrollAreaHeight, scrollAreaHeight * visible / total));
        int maxOffset = Math.max(1, total - visible);
        int thumbY = scrollAreaY + (scrollAreaHeight - thumbHeight) * offset / maxOffset;
        int thumbSourceY = sourceScrollY + Math.max(0, (sourceScrollHeight - thumbSourceHeight) / 2);
        graphics.blit(SPELLCRAFT_ATLAS, x, thumbY, width, thumbHeight,
                u, thumbSourceY, sourceWidth, thumbSourceHeight, 1536, 1024);
    }

    private static int effectX(int index) {
        return EFFECT_X + (index % EFFECT_COLUMNS) * (BUTTON_W + GAP);
    }

    private static int effectY(int index) {
        return EFFECT_Y + (index / EFFECT_COLUMNS) * (BUTTON_H + GAP);
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

    private static ResourceLocation tex(String path) {
        return new ResourceLocation(MOD_ID, "textures/gui/spellcraft/generated/" + path);
    }

    private static ResourceLocation effectIcon(String effect) {
        return tex("icons/effects/" + normalize(effect) + ".png");
    }

    private record RequirementIcon(int x, int y, SpellIngredients.Requirement requirement, int amount) {}

    private record AtlasIcon(int u, int v, int w, int h) {}

    private static AtlasIcon shapeAtlasIcon(String shape) {
        return switch (normalize(shape)) {
            case "self" -> new AtlasIcon(86, 106, 165, 163);
            case "target" -> new AtlasIcon(376, 106, 161, 163);
            case "point" -> new AtlasIcon(677, 106, 136, 167);
            case "block" -> new AtlasIcon(949, 107, 153, 161);
            case "target_aoe" -> new AtlasIcon(374, 426, 165, 160);
            case "aoe" -> new AtlasIcon(950, 427, 150, 161);
            case "ally_aoe" -> new AtlasIcon(1235, 446, 162, 124);
            case "ally_target_aoe" -> new AtlasIcon(85, 743, 165, 161);
            case "items_self_aoe" -> new AtlasIcon(375, 760, 163, 137);
            case "water_target_aoe" -> new AtlasIcon(661, 745, 167, 161);
            default -> new AtlasIcon(86, 106, 165, 163);
        };
    }

    private static void blitShapeIcon(GuiGraphics graphics, String shape, int x, int y, int size) {
        AtlasIcon icon = shapeAtlasIcon(shape);
        int drawWidth = size;
        int drawHeight = size;
        if (icon.w() > icon.h()) {
            drawHeight = Math.max(1, size * icon.h() / icon.w());
        } else if (icon.h() > icon.w()) {
            drawWidth = Math.max(1, size * icon.w() / icon.h());
        }
        graphics.blit(SHAPE_ICONS_ATLAS, x + (size - drawWidth) / 2, y + (size - drawHeight) / 2, drawWidth, drawHeight,
                icon.u(), icon.v(), icon.w(), icon.h(), 1536, 1024);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace(' ', '_').replace('-', '_');
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
    }

    private class MagicButton extends Button {
        private boolean selected;
        private boolean goldFrame;
        private ResourceLocation icon;
        private String shapeIconKey;
        private int actionSpriteU = -1;

        MagicButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        void setGoldFrame(boolean goldFrame) {
            this.goldFrame = goldFrame;
        }

        void setIcon(ResourceLocation icon) {
            this.icon = icon;
            this.shapeIconKey = null;
        }

        void setShapeIcon(String shapeIconKey) {
            this.shapeIconKey = shapeIconKey;
            this.icon = null;
        }

        void setActionSprite(int actionSpriteU) {
            this.actionSpriteU = actionSpriteU;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (goldFrame && actionSpriteU >= 0) {
                int v = !active ? ACTION_DISABLED_V : isHoveredOrFocused() ? ACTION_SELECTED_V : ACTION_AVAILABLE_V;
                graphics.blit(SPELLCRAFT_ATLAS, getX(), getY(), width, height, actionSpriteU, v, ACTION_SOURCE_SIZE, ACTION_SOURCE_SIZE, 1536, 1024);
                return;
            }

            ResourceLocation texture;
            int texW = width;
            int texH = height;
            if (!active) {
                texture = width > BUTTON_W ? BUTTON_SHAPE_DISABLED : BUTTON_DISABLED;
                texW = width > BUTTON_W ? 124 : 112;
                texH = 32;
            } else if (goldFrame && icon != null) {
                texture = icon;
                texW = ACTION_SIZE;
                texH = ACTION_SIZE;
            } else if (width > BUTTON_W) {
                texture = selected || isHoveredOrFocused() ? BUTTON_SHAPE_SELECTED : BUTTON_SHAPE_NORMAL;
                texW = 124;
                texH = 32;
            } else {
                texture = selected || isHoveredOrFocused() ? BUTTON_SELECTED : BUTTON_NORMAL;
                texW = 112;
                texH = 32;
            }
            blit(graphics, texture, getX(), getY(), width, height, texW, texH);

            if (!active) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x99000000);
            }

            String text = getMessage().getString();
            if (goldFrame && icon != null) {
                return;
            }
            int color = active ? (goldFrame ? COLOR_GOLD : selected ? COLOR_TEXT : 0xFFD7C3E8) : 0xFF6A6375;
            boolean hasAnyIcon = icon != null || shapeIconKey != null;
            int iconSize = 18;
            int iconX = getX() + 11;
            int iconY = getY() + (height - iconSize) / 2;
            if (icon != null) {
                blit(graphics, icon, iconX, iconY, iconSize, iconSize, 16, 16);
            }
            if (shapeIconKey != null) {
                blitShapeIcon(graphics, shapeIconKey, iconX, iconY, iconSize);
            }
            int textX = !hasAnyIcon ? getX() + width / 2 - font.width(text) / 2 : getX() + 37;
            int textY = getY() + (height - font.lineHeight) / 2;
            graphics.drawString(font, text, textX, textY, color, false);
        }
    }
}
