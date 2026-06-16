package com.ourmagic.client;

import com.ourmagic.item.GrimoireItem;
import com.ourmagic.magic.SpellIngredients;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GrimoireScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 220;
    private static final int LIST_W = 146;
    private static final int ROW_H = 18;

    private final ItemStack grimoire;
    private final List<SpellInstance> spells;
    private EditBox searchBox;
    private int scrollOffset;
    private int selectedIndex;

    public GrimoireScreen(ItemStack grimoire) {
        super(Component.literal("Grimoire"));
        this.grimoire = grimoire.copy();
        this.spells = GrimoireItem.spellInstances(this.grimoire);
    }

    public static void open(ItemStack grimoire) {
        Minecraft.getInstance().setScreen(new GrimoireScreen(grimoire));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        searchBox = new EditBox(font, left + 14, top + 32, LIST_W - 12, 18, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search"));
        searchBox.setResponder(value -> {
            scrollOffset = 0;
            selectedIndex = 0;
        });
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE12091B);
        graphics.fill(left + 4, top + 4, left + PANEL_W - 4, top + PANEL_H - 4, 0xDD1C102A);
        graphics.hLine(left + 10, left + PANEL_W - 10, top + 27, 0xFF8E62C8);

        graphics.drawString(font, "Grimoire", left + 14, top + 12, 0xFFEBD6FF, false);
        graphics.drawString(font, "Magic " + GrimoireItem.magicCharge(grimoire) + "/1000", left + PANEL_W - 102, top + 12, 0xFFC66CFF, false);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderSpellList(graphics, left, top, mouseX, mouseY);
        renderSpellDetails(graphics, left, top);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        List<SpellInstance> filtered = filteredSpells();
        int listX = left + 14;
        int listY = top + 58;
        int visibleRows = visibleRows();
        if (mouseX >= listX && mouseX < listX + LIST_W && mouseY >= listY && mouseY < listY + visibleRows * ROW_H) {
            int row = ((int) mouseY - listY) / ROW_H;
            int index = scrollOffset + row;
            if (index >= 0 && index < filtered.size()) {
                selectedIndex = index;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, filteredSpells().size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(delta)));
        selectedIndex = Math.max(0, Math.min(selectedIndex, Math.max(0, filteredSpells().size() - 1)));
        return true;
    }

    private void renderSpellList(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        List<SpellInstance> filtered = filteredSpells();
        int listX = left + 14;
        int listY = top + 58;
        int visibleRows = visibleRows();
        graphics.fill(listX - 2, listY - 2, listX + LIST_W, listY + visibleRows * ROW_H + 2, 0xAA08040D);

        for (int row = 0; row < visibleRows; row++) {
            int index = scrollOffset + row;
            if (index >= filtered.size()) {
                break;
            }
            SpellInstance spell = filtered.get(index);
            int y = listY + row * ROW_H;
            boolean selected = index == selectedIndex;
            boolean hovered = mouseX >= listX && mouseX < listX + LIST_W && mouseY >= y && mouseY < y + ROW_H;
            if (selected || hovered) {
                graphics.fill(listX, y, listX + LIST_W - 4, y + ROW_H - 1, selected ? 0x884F2B78 : 0x553A2358);
            }
            graphics.drawString(font, compact(spell.displayName(), 19), listX + 4, y + 5,
                    selected ? 0xFF55E8FF : 0xFFD7C3E8, false);
        }

        if (filtered.isEmpty()) {
            graphics.drawString(font, "No spells", listX + 4, listY + 6, 0xFFFF7777, false);
        }
    }

    private void renderSpellDetails(GuiGraphics graphics, int left, int top) {
        List<SpellInstance> filtered = filteredSpells();
        int x = left + 178;
        int y = top + 38;
        graphics.fill(x - 6, y - 6, left + PANEL_W - 14, top + PANEL_H - 14, 0x9908040D);
        if (filtered.isEmpty()) {
            graphics.drawString(font, "No matching spells", x, y, 0xFFFF7777, false);
            return;
        }

        SpellInstance spell = filtered.get(Math.max(0, Math.min(selectedIndex, filtered.size() - 1)));
        graphics.drawString(font, compact(spell.displayName(), 24), x, y, 0xFFFFD84A, false);
        graphics.drawString(font, spell.key(), x, y + 16, 0xFFBFA7D8, false);
        graphics.drawString(font, "Payloads: " + SpellRegistry.payloadKey(spell.key()), x, y + 38, 0xFF55E8FF, false);
        graphics.drawString(font, "Shape: " + SpellRegistry.shapeKey(spell.key()), x, y + 52, 0xFF67FF61, false);
        graphics.drawString(font, "Mana: " + spell.manaCost(), x, y + 74, 0xFF80A8FF, false);
        graphics.drawString(font, String.format(Locale.ROOT, "Cooldown: %.1fs", spell.cooldownTicks() / 20.0F), x, y + 88, 0xFFFFD84A, false);

        graphics.drawString(font, "Recipe", x, y + 112, 0xFFEBD6FF, false);
        List<SpellIngredients.Requirement> requirements = SpellIngredients.requirementsFor(spell.key());
        int requirementY = y + 128;
        for (int i = 0; i < Math.min(4, requirements.size()); i++) {
            SpellIngredients.Requirement requirement = requirements.get(i);
            graphics.renderItem(requirement.display(), x, requirementY + i * 18 - 4);
            graphics.drawString(font, requirement.count() + "x " + compact(requirement.displayName().getString(), 20),
                    x + 20, requirementY + i * 18, 0xFFD7C3E8, false);
        }
        if (requirements.size() > 4) {
            graphics.drawString(font, "...", x + 20, requirementY + 72, 0xFF9B85AD, false);
        }
    }

    private List<SpellInstance> filteredSpells() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return spells;
        }
        List<SpellInstance> filtered = new ArrayList<>();
        for (SpellInstance spell : spells) {
            if (spell.displayName().toLowerCase(Locale.ROOT).contains(query)
                    || spell.key().toLowerCase(Locale.ROOT).contains(query)
                    || SpellRegistry.payloadKey(spell.key()).toLowerCase(Locale.ROOT).contains(query)
                    || SpellRegistry.shapeKey(spell.key()).toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(spell);
            }
        }
        return filtered;
    }

    private int visibleRows() {
        return 8;
    }

    private static String compact(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + ".";
    }
}
