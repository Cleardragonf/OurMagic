package com.ourmagic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class GuideBookScreen extends Screen {
    private static final int WIDTH = 342;
    private static final int HEIGHT = 214;
    private static final int PAGE_W = 153;
    private static final int PAGE_H = 190;
    private static final int INK = 0xFF302033;
    private static final int MUTED_INK = 0xFF6B5570;
    private static final int GOLD = 0xFF9B6B24;
    private static final int PURPLE = 0xFF7D3FA6;
    private static final List<Chapter> CHAPTERS = List.of(
            new Chapter("Wands", "Casting and HUD", List.of(
                    new Page("Casting", "Using your active spell", List.of(
                            "Hold a wand and right-click to cast.",
                            "Casting spends mana and starts that spell cooldown.",
                            "The active spell, cost, and mana bar appear above the vanilla HUD."
                    )),
                    new Page("Hot Spells", "Fast selection", List.of(
                            "A wand has six hot spell slots.",
                            "Press 1-6 while holding a wand to select one.",
                            "These keys select wand spells without switching your hotbar item."
                    )),
                    new Page("Editing Slots", "Shift cursor mode", List.of(
                            "Hold Shift with a wand to show the spell list.",
                            "Click a spell, then press 1-6 to assign it.",
                            "Without Shift, mouse wheel scrolling stays vanilla."
                    ))
            )),
            new Chapter("Finding", "Learning spells", List.of(
                    new Page("Wands", "Discovery by loot or craft", List.of(
                            "Crafted and found wands can roll different spells.",
                            "Combining wands on an anvil can merge known spells.",
                            "Higher-level spells are useful sources for stronger crafting."
                    )),
                    new Page("Knowledge", "Books and grimoires", List.of(
                            "Arcane Knowledge books can reveal recipes.",
                            "Grimoires store crafted spells and can provide known recipes.",
                            "Spell paper is a portable recipe result from Spellcraft."
                    ))
            )),
            new Chapter("Spellcraft", "Building spells", List.of(
                    new Page("Opening", "Where to craft", List.of(
                            "Use a wand on an enchanting table to open Spellcraft.",
                            "Choose one or more effects, then choose a shape.",
                            "The preview shows what will be crafted."
                    )),
                    new Page("Effects", "What the spell does", List.of(
                            "Effects are payloads like Arrow, Lightning, Heal, or Shield.",
                            "Your wand level controls how many effects can be combined.",
                            "Unavailable effects are hidden until you have a recipe source."
                    )),
                    new Page("Shapes", "How the spell travels", List.of(
                            "Shapes decide delivery: target, area, point, block, and more.",
                            "Only shapes compatible with the selected effects are shown.",
                            "Shapes also require matching talisman items."
                    )),
                    new Page("Outputs", "Where the result goes", List.of(
                            "Add to Wand teaches the held wand.",
                            "Make Spell Paper creates a portable spell recipe.",
                            "Add to Grimoire stores the spell in a grimoire."
                    ))
            )),
            new Chapter("Ingredients", "Costs and requirements", List.of(
                    new Page("Reading Costs", "Icons over walls of text", List.of(
                            "Spellcraft shows required items as icons.",
                            "Hover an icon to see the item name and count.",
                            "Green counts are ready; red counts need more items."
                    )),
                    new Page("Effect Items", "Payload materials", List.of(
                            "Each effect has its own reagent list.",
                            "Arrow needs arrows and feathers; Lightning uses a talisman.",
                            "More advanced effects tend to need rarer ingredients."
                    )),
                    new Page("Shape Items", "Talisman gates", List.of(
                            "Target uses Seeker; Self uses Anchor.",
                            "Area shapes use stronger talismans and may need multiples.",
                            "Having at least one shape item lets that shape appear."
                    ))
            )),
            new Chapter("Upgrades", "Growing stronger", List.of(
                    new Page("Spell Upgrades", "Main-hand wand and anvil", List.of(
                            "Use a wand on an anvil from the main hand.",
                            "Pick a spell, then spend spell points on upgrades.",
                            "Upgrades can increase damage, range, radius, duration, or chaining."
                    )),
                    new Page("Magic Levels", "Player progression", List.of(
                            "Successful casts give spell XP to the wand spell.",
                            "You also gain about 10 percent of that as magic XP.",
                            "Magic levels grant magic points for player upgrades."
                    )),
                    new Page("Player Upgrades", "Offhand wand and anvil", List.of(
                            "Hold a wand in the offhand and use an anvil.",
                            "Spend magic points on Max Mana or Mana Regeneration.",
                            "These upgrades stay with your player."
                    ))
            ))
    );

    private int chapter;
    private int page;

    private GuideBookScreen() {
        super(Component.literal("OurMagic Guide"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideBookScreen());
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        for (int i = 0; i < CHAPTERS.size(); i++) {
            int index = i;
            BookmarkButton tab = new BookmarkButton(left - 20, top + 28 + i * 25, index, b -> {
                chapter = index;
                page = 0;
                rebuildButtons();
            });
            tab.setTooltip(Tooltip.create(Component.literal(CHAPTERS.get(index).title())));
            addRenderableWidget(tab);
        }

        addRenderableWidget(new PageArrowButton(left + 26, top + HEIGHT - 25, false, b -> {
            previousPage();
            rebuildButtons();
        }));
        addRenderableWidget(new PageArrowButton(left + WIDTH - 48, top + HEIGHT - 25, true, b -> {
            nextPage();
            rebuildButtons();
        }));
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(left + WIDTH / 2 - 24, top + HEIGHT - 24, 48, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        drawBook(graphics, left, top);
        renderLeftPage(graphics, left + 18, top + 17);
        renderRightPage(graphics, left + 174, top + 17);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBook(GuiGraphics graphics, int left, int top) {
        graphics.fill(left + 2, top + 6, left + WIDTH - 2, top + HEIGHT, 0x88000000);
        graphics.fill(left + 8, top + 8, left + WIDTH - 8, top + HEIGHT - 8, 0xFF5A2D63);
        graphics.fill(left + 12, top + 12, left + WIDTH - 12, top + HEIGHT - 12, 0xFF2A1734);

        drawPage(graphics, left + 16, top + 12, true);
        drawPage(graphics, left + 172, top + 12, false);

        graphics.fill(left + WIDTH / 2 - 4, top + 13, left + WIDTH / 2 + 4, top + HEIGHT - 13, 0xFF261125);
        graphics.fill(left + WIDTH / 2 - 1, top + 18, left + WIDTH / 2 + 1, top + HEIGHT - 18, 0x665C3560);
        graphics.drawCenteredString(font, title, left + WIDTH / 2, top + 2, 0xFFE9C8FF);
    }

    private void drawPage(GuiGraphics graphics, int x, int y, boolean leftPage) {
        graphics.fill(x, y, x + PAGE_W, y + PAGE_H, 0xFFE3C992);
        graphics.fill(x + 3, y + 3, x + PAGE_W - 3, y + PAGE_H - 3, 0xFFF1DBA8);
        graphics.fill(x + (leftPage ? PAGE_W - 10 : 8), y + 7, x + (leftPage ? PAGE_W - 8 : 10), y + PAGE_H - 7, 0x22A16F3F);
        graphics.fill(x + 9, y + 12, x + PAGE_W - 9, y + 13, 0x449B6B24);
        graphics.fill(x + 9, y + PAGE_H - 13, x + PAGE_W - 9, y + PAGE_H - 12, 0x449B6B24);
    }

    private void renderLeftPage(GuiGraphics graphics, int x, int y) {
        graphics.drawCenteredString(font, "Chapters", x + PAGE_W / 2, y + 6, GOLD);
        int itemY = y + 28;
        for (int i = 0; i < CHAPTERS.size(); i++) {
            Chapter entry = CHAPTERS.get(i);
            int color = i == chapter ? PURPLE : INK;
            graphics.drawString(font, (i + 1) + ". " + entry.title(), x + 14, itemY, color, false);
            graphics.drawString(font, entry.summary(), x + 22, itemY + 11, MUTED_INK, false);
            itemY += 29;
        }
        Chapter current = CHAPTERS.get(chapter);
        graphics.drawCenteredString(font, "Page " + (page + 1) + " of " + current.pages().size(), x + PAGE_W / 2, y + PAGE_H - 24, MUTED_INK);
    }

    private void renderRightPage(GuiGraphics graphics, int x, int y) {
        Chapter currentChapter = CHAPTERS.get(chapter);
        Page current = currentChapter.pages().get(page);
        graphics.drawCenteredString(font, current.title(), x + PAGE_W / 2, y + 6, PURPLE);
        graphics.drawCenteredString(font, current.summary(), x + PAGE_W / 2, y + 20, GOLD);

        int textY = y + 44;
        for (String line : current.lines()) {
            List<FormattedCharSequence> wrapped = font.split(Component.literal("- " + line), PAGE_W - 28);
            for (FormattedCharSequence row : wrapped) {
                graphics.drawString(font, row, x + 14, textY, INK, false);
                textY += 10;
            }
            textY += 8;
        }
        graphics.drawCenteredString(font, currentChapter.title() + " " + (page + 1) + "/" + currentChapter.pages().size(), x + PAGE_W / 2, y + PAGE_H - 23, MUTED_INK);
    }

    private void previousPage() {
        if (page > 0) {
            page--;
            return;
        }
        if (chapter > 0) {
            chapter--;
            page = CHAPTERS.get(chapter).pages().size() - 1;
        }
    }

    private void nextPage() {
        if (page < CHAPTERS.get(chapter).pages().size() - 1) {
            page++;
            return;
        }
        if (chapter < CHAPTERS.size() - 1) {
            chapter++;
            page = 0;
        }
    }

    private boolean hasPreviousPage() {
        return page > 0 || chapter > 0;
    }

    private boolean hasNextPage() {
        return page < CHAPTERS.get(chapter).pages().size() - 1 || chapter < CHAPTERS.size() - 1;
    }

    private record Chapter(String title, String summary, List<Page> pages) {
    }

    private record Page(String title, String summary, List<String> lines) {
    }

    private final class BookmarkButton extends Button {
        private final int index;

        private BookmarkButton(int x, int y, int index, OnPress onPress) {
            super(x, y, 24, 20, Component.literal(String.valueOf(index + 1)), onPress, DEFAULT_NARRATION);
            this.index = index;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = index == chapter ? 0xFFE9B85A : isHoveredOrFocused() ? 0xFFD080C8 : 0xFF7C3E96;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF2A1734);
            graphics.fill(getX() + 2, getY() + 2, getX() + width, getY() + height - 2, color);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2 + 1, getY() + 6, 0xFF1C1020);
        }
    }

    private final class PageArrowButton extends Button {
        private final boolean next;

        private PageArrowButton(int x, int y, boolean next, OnPress onPress) {
            super(x, y, 22, 18, Component.literal(next ? ">" : "<"), onPress, DEFAULT_NARRATION);
            this.next = next;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean disabled = next ? !hasNextPage() : !hasPreviousPage();
            active = !disabled;
            int color = disabled ? 0xFF7B6A59 : isHoveredOrFocused() ? 0xFFB070D0 : GOLD;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x66302020);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 5, color);
        }
    }
}
