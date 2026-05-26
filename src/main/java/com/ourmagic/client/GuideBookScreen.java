package com.ourmagic.client;

import com.ourmagic.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class GuideBookScreen extends Screen {
    private static final int WIDTH = 342;
    private static final int HEIGHT = 214;
    private static final int PAGE_W = 153;
    private static final int PAGE_H = 190;
    private static final int INK = 0xFF180D1B;
    private static final int MUTED_INK = 0xFF4A3652;
    private static final int GOLD = 0xFF6F430E;
    private static final int PURPLE = 0xFF5B147F;
    private static final List<Chapter> CHAPTERS = List.of(
            new Chapter("Wands", "Using magic", List.of(
                    new Page("First Steps", "What a wand is", List.of(
                            "A wand is your main casting tool. Hold it in either hand to use known spells.",
                            "Every normal wand has its own spell list, power value, cooldowns, and hot slots.",
                            "Crafted wands are random, so two wands made from the same items may be different."
                    )),
                    new Page("Casting", "Using the selected spell", List.of(
                            "Hold a wand and right-click to cast the active spell.",
                            "Casting spends mana and puts that spell on cooldown.",
                            "The HUD shows the active spell, mana cost, cooldown, and your current mana."
                    )),
                    new Page("Hot Spells", "Fast selection", List.of(
                            "Each wand has six hot spell slots for spells you use often.",
                            "Press 1-6 while holding a wand to select one.",
                            "These keys select wand spells without changing your normal hotbar item."
                    )),
                    new Page("Editing Slots", "Shift cursor mode", List.of(
                            "Hold Shift while holding a wand to open its spell list cursor.",
                            "Click a spell in the list, then press 1-6 to place it in that hot slot.",
                            "Release Shift to return to normal movement and vanilla mouse wheel scrolling."
                    )),
                    new Page("Focus Cast", "Careful aiming", List.of(
                            "Some spells benefit from focused casting when you take time to aim.",
                            "Use the focus controls shown by the HUD to charge a steadier cast.",
                            "Focus is most useful for range, damage, radius, duration, or utility-heavy spells."
                    )),
                    new Page("Chant Cast", "Charged words", List.of(
                            "Chanting lets a wand build a stronger cast before release.",
                            "The chant HUD shows the current phrase and charge progress.",
                            "Cancel the chant if the target changes or you no longer want to spend the mana."
                    ))
            )),
            new Chapter("Crafting", "Making wands", List.of(
                    new Page("Basic Wand", "Exact ingredients", List.of(
                            "Craft a wand with exactly 2 sticks, 1 amethyst shard, and 1 glowstone dust.",
                            "The recipe is shapeless. Put the four items anywhere in a 2x2 or 3x3 grid.",
                            "No extra items may be in the grid, or the recipe will not match."
                    ), PageVisual.WAND_SHAPELESS),
                    new Page("Example Layout", "One valid pattern", List.of(
                            "Top left: amethyst shard. Top right: glowstone dust.",
                            "Middle left: stick. Bottom left: stick.",
                            "This is only an example. Any arrangement works if the item counts are correct."
                    ), PageVisual.WAND_GRID),
                    new Page("What Rolls", "Why wands differ", List.of(
                            "A crafted wand rolls a starting spell from the spell registry.",
                            "It also rolls a power value, so some wands naturally cast stronger than others.",
                            "A small number of crafted wands begin with extra known spells."
                    )),
                    new Page("Rerolling", "Trying for better starts", List.of(
                            "Craft more wands when you want a different starting spell.",
                            "Keep useful rolls even if they are not your main wand.",
                            "Spare wands can be merged later to collect their spells."
                    )),
                    new Page("Combining", "Anvil wand merging", List.of(
                            "Put two wands on an anvil to create a combined wand.",
                            "The result keeps known spells from both wands when possible.",
                            "This is the easiest way to build one wand with many useful options."
                    ), PageVisual.WAND_COMBINE),
                    new Page("Modifiers", "Crafting upgrades", List.of(
                            "Craft a wand together with one modifier item to add that modifier.",
                            "The wand keeps its spells and gains one level of that modifier.",
                            "A modifier can only be added if the wand can still accept more modifier levels."
                    ), PageVisual.WAND_MODIFIER),
                    new Page("Modifier Items", "What each item means", List.of(
                            "Hover each item to see its modifier.",
                            "The tooltip shows the per-level gain and the practical range."
                    ), PageVisual.MODIFIER_ITEMS),
                    new Page("Modifier Effects", "How they help", List.of(
                            "Haste lowers cooldowns. Focus lowers mana cost and improves focus power.",
                            "Potency improves damage. Hardness improves non-damage utility.",
                            "Elasticity improves range and radius. Wisdom improves spell XP gain."
                    ))
            )),
            new Chapter("Learning", "Finding spells", List.of(
                    new Page("Sources", "Where recipes come from", List.of(
                            "Crafted and found wands can teach you what spells exist.",
                            "Arcane Knowledge books can reveal spell recipes.",
                            "Grimoires store crafted spells and can provide known recipes later."
                    )),
                    new Page("Knowledge", "Books and grimoires", List.of(
                            "Use Arcane Knowledge when you want to discover a recipe.",
                            "Use a grimoire when you want a permanent spell library.",
                            "Use spell paper when you want a portable recipe result from Spellcraft."
                    )),
                    new Page("Spell Paper", "Portable recipes", List.of(
                            "Spell paper carries one crafted spell recipe.",
                            "Use it to teach a wand, feed a ward, or save a useful build for later.",
                            "Paper is helpful when you are not ready to commit a spell to your main wand."
                    ))
            )),
            new Chapter("Spellcraft", "Building spells", List.of(
                    new Page("Opening", "Where to craft", List.of(
                            "Use a wand on an enchanting table to open Spellcraft.",
                            "Choose one or more effects, then choose a shape.",
                            "The preview shows what will be crafted."
                    )),
                    new Page("Effects", "What the spell does", List.of(
                            "Effects are payloads like Arrow, Lightning, Heal, Fortify, or Shield.",
                            "Your wand level controls how many effects can be combined.",
                            "Unavailable effects are hidden until you have a recipe source."
                    )),
                    new Page("Defenses", "Protective magic", List.of(
                            "Fortify, Fireguard, Fallguard, Blastguard, Agility, Mana Shield, and Life Ward are defensive effects.",
                            "Some grant vanilla buffs. Mana Shield and Life Ward use OurMagic status logic.",
                            "Fallguard blocks fall damage. Blastguard blocks explosion damage."
                    )),
                    new Page("Combos", "Multiple effects", List.of(
                            "A spell can contain more than one effect when your wand level allows it.",
                            "Combined effects share one shape, so choose effects that make sense together.",
                            "Complex spells cost more materials and often more mana."
                    )),
                    new Page("Shapes", "How the spell travels", List.of(
                            "Shapes decide delivery: target, area, point, block, and more.",
                            "Only shapes compatible with the selected effects are shown.",
                            "Shapes also require matching talisman items."
                    )),
                    new Page("Preview", "Check before crafting", List.of(
                            "The Spellcraft preview shows the final spell before you spend materials.",
                            "Check the effect list, shape, mana cost, and cooldown.",
                            "If something is missing, review the red ingredient counts."
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
                    new Page("Talisman Idea", "Shape unlocks", List.of(
                            "Talismans act like keys for spell shapes.",
                            "If a shape is missing, you probably do not have its talisman.",
                            "Keep spare talismans near your enchanting table for easier crafting."
                    )),
                    new Page("Shape Items", "Talisman gates", List.of(
                            "Target uses Seeker; Self uses Anchor.",
                            "Area shapes use stronger talismans and may need multiples.",
                            "Having at least one shape item lets that shape appear."
                    )),
                    new Page("Reading Failure", "When a button is disabled", List.of(
                            "A disabled craft action usually means missing items or no valid output.",
                            "Hover item icons to see exact names and counts.",
                            "Try a simpler effect or shape when early-game supplies are limited."
                    ))
            )),
            new Chapter("Upgrades", "Growing stronger", List.of(
                    new Page("Spell Upgrades", "Main-hand wand and anvil", List.of(
                            "Use a wand on an anvil from the main hand.",
                            "Pick a spell, then spend spell points on upgrades.",
                            "Upgrades can increase damage, healing, range, radius, duration, or chaining."
                    )),
                    new Page("Magic Levels", "Player progression", List.of(
                            "Successful casts give spell XP to the wand spell.",
                            "You also gain about 10 percent of that as magic XP.",
                            "Magic levels grant magic points for player upgrades."
                    )),
                    new Page("Wand Level", "Unlocking complexity", List.of(
                            "A wand's level grows as its spells are used.",
                            "Higher wand levels support more ambitious spellcraft.",
                            "Keep casting useful spells instead of leaving them unused in storage."
                    )),
                    new Page("Player Upgrades", "Offhand wand and anvil", List.of(
                            "Hold a wand in the offhand and use an anvil.",
                            "Spend magic points on Max Mana or Mana Regeneration.",
                            "These upgrades stay with your player."
                    )),
                    new Page("Upgrade Advice", "Choosing first", List.of(
                            "Max Mana helps with expensive spells and long fights.",
                            "Mana Regeneration helps frequent casting and exploration.",
                            "Spell upgrades are best spent on spells you actually keep equipped."
                    ))
            ))
    );

    private int chapter;
    private int page;
    private List<Component> hoveredTooltip = List.of();

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
        hoveredTooltip = List.of();
        renderBackground(graphics);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        drawBook(graphics, left, top);
        renderLeftPage(graphics, left + 18, top + 17);
        renderRightPage(graphics, left + 174, top + 17, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoveredTooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, hoveredTooltip, mouseX, mouseY);
        }
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
        int itemY = y + 25;
        int spacing = CHAPTERS.size() > 5 ? 23 : 29;
        for (int i = 0; i < CHAPTERS.size(); i++) {
            Chapter entry = CHAPTERS.get(i);
            int color = i == chapter ? PURPLE : INK;
            graphics.drawString(font, (i + 1) + ". " + entry.title(), x + 14, itemY, color, false);
            graphics.drawString(font, entry.summary(), x + 22, itemY + 10, MUTED_INK, false);
            itemY += spacing;
        }
        Chapter current = CHAPTERS.get(chapter);
        graphics.drawCenteredString(font, "Page " + (page + 1) + " of " + current.pages().size(), x + PAGE_W / 2, y + PAGE_H - 24, MUTED_INK);
    }

    private void renderRightPage(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        Chapter currentChapter = CHAPTERS.get(chapter);
        Page current = currentChapter.pages().get(page);
        graphics.drawCenteredString(font, current.title(), x + PAGE_W / 2, y + 6, PURPLE);
        graphics.drawCenteredString(font, current.summary(), x + PAGE_W / 2, y + 20, GOLD);

        int textY = y + 44;
        if (current.visual() != PageVisual.NONE) {
            textY = renderPageVisual(graphics, current.visual(), x, textY, mouseX, mouseY);
        }
        int bottom = y + PAGE_H - 35;
        for (String line : current.lines()) {
            List<FormattedCharSequence> wrapped = font.split(Component.literal("- " + line), PAGE_W - 28);
            for (FormattedCharSequence row : wrapped) {
                if (textY > bottom) {
                    break;
                }
                graphics.drawString(font, row, x + 14, textY, INK, false);
                textY += 10;
            }
            textY += 5;
            if (textY > bottom) {
                break;
            }
        }
        graphics.drawCenteredString(font, currentChapter.title() + " " + (page + 1) + "/" + currentChapter.pages().size(), x + PAGE_W / 2, y + PAGE_H - 23, MUTED_INK);
    }

    private int renderPageVisual(GuiGraphics graphics, PageVisual visual, int x, int y, int mouseX, int mouseY) {
        return switch (visual) {
            case WAND_SHAPELESS -> renderWandShapeless(graphics, x, y);
            case WAND_GRID -> renderWandGrid(graphics, x, y);
            case WAND_COMBINE -> renderWandCombine(graphics, x, y);
            case WAND_MODIFIER -> renderWandModifier(graphics, x, y);
            case MODIFIER_ITEMS -> renderModifierItems(graphics, x, y, mouseX, mouseY);
            case NONE -> y;
        };
    }

    private int renderWandShapeless(GuiGraphics graphics, int x, int y) {
        int startX = x + 12;
        drawItemBox(graphics, new ItemStack(Items.STICK, 2), startX, y);
        drawPlus(graphics, startX + 21, y + 5);
        drawItemBox(graphics, new ItemStack(Items.AMETHYST_SHARD), startX + 32, y);
        drawPlus(graphics, startX + 53, y + 5);
        drawItemBox(graphics, new ItemStack(Items.GLOWSTONE_DUST), startX + 64, y);
        drawArrow(graphics, startX + 88, y + 7);
        drawItemBox(graphics, new ItemStack(ModItems.WAND.get()), startX + 111, y);
        graphics.drawCenteredString(font, "shapeless", x + PAGE_W / 2, y + 25, MUTED_INK);
        return y + 43;
    }

    private int renderWandGrid(GuiGraphics graphics, int x, int y) {
        int gridX = x + 23;
        drawGridSlot(graphics, new ItemStack(Items.AMETHYST_SHARD), gridX, y);
        drawGridSlot(graphics, new ItemStack(Items.GLOWSTONE_DUST), gridX + 19, y);
        drawGridSlot(graphics, ItemStack.EMPTY, gridX + 38, y);
        drawGridSlot(graphics, new ItemStack(Items.STICK), gridX, y + 19);
        drawGridSlot(graphics, ItemStack.EMPTY, gridX + 19, y + 19);
        drawGridSlot(graphics, ItemStack.EMPTY, gridX + 38, y + 19);
        drawGridSlot(graphics, new ItemStack(Items.STICK), gridX, y + 38);
        drawGridSlot(graphics, ItemStack.EMPTY, gridX + 19, y + 38);
        drawGridSlot(graphics, ItemStack.EMPTY, gridX + 38, y + 38);
        drawArrow(graphics, x + 91, y + 24);
        drawItemBox(graphics, new ItemStack(ModItems.WAND.get()), x + 116, y + 20);
        return y + 67;
    }

    private int renderWandCombine(GuiGraphics graphics, int x, int y) {
        int startX = x + 20;
        drawItemBox(graphics, new ItemStack(ModItems.WAND.get()), startX, y);
        drawPlus(graphics, startX + 23, y + 5);
        drawItemBox(graphics, new ItemStack(ModItems.WAND.get()), startX + 36, y);
        drawArrow(graphics, startX + 64, y + 7);
        drawItemBox(graphics, new ItemStack(ModItems.WAND.get()), startX + 88, y);
        graphics.drawCenteredString(font, "anvil merge", x + PAGE_W / 2, y + 25, MUTED_INK);
        return y + 43;
    }

    private int renderWandModifier(GuiGraphics graphics, int x, int y) {
        int startX = x + 17;
        drawItemBox(graphics, new ItemStack(ModItems.WAND.get()), startX, y);
        drawPlus(graphics, startX + 23, y + 5);
        drawItemBox(graphics, new ItemStack(Items.REDSTONE), startX + 36, y);
        drawArrow(graphics, startX + 64, y + 7);
        drawItemBox(graphics, new ItemStack(ModItems.WAND.get()), startX + 88, y);
        graphics.drawCenteredString(font, "wand + modifier", x + PAGE_W / 2, y + 25, MUTED_INK);
        return y + 43;
    }

    private int renderModifierItems(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        drawLabeledModifier(graphics, new ItemStack(Items.DIAMOND), "Hard", x + 12, y, mouseX, mouseY, List.of(
                Component.literal("Hardness"),
                Component.literal("Level 1: utility duration 1.10x"),
                Component.literal("Each level adds another +10%"),
                Component.literal("No hard cap except wand slots")
        ));
        drawLabeledModifier(graphics, new ItemStack(Items.REDSTONE), "Haste", x + 53, y, mouseX, mouseY, List.of(
                Component.literal("Haste"),
                Component.literal("Level 1: cooldown 0.92x"),
                Component.literal("Each level removes another 8%"),
                Component.literal("Minimum cooldown: 0.35x")
        ));
        drawLabeledModifier(graphics, new ItemStack(Items.QUARTZ), "Focus", x + 94, y, mouseX, mouseY, List.of(
                Component.literal("Focus"),
                Component.literal("Level 1: mana 0.94x, range 1.05x"),
                Component.literal("Mana drops 6% per level"),
                Component.literal("Minimum mana: 0.35x")
        ));
        drawLabeledModifier(graphics, new ItemStack(Items.NETHERITE_INGOT), "Power", x + 12, y + 33, mouseX, mouseY, List.of(
                Component.literal("Potency"),
                Component.literal("Level 1: damage 1.12x"),
                Component.literal("Each level adds another +12%"),
                Component.literal("No hard cap except wand slots")
        ));
        drawLabeledModifier(graphics, new ItemStack(Items.SLIME_BALL), "Range", x + 53, y + 33, mouseX, mouseY, List.of(
                Component.literal("Elasticity"),
                Component.literal("Level 1: radius 1.10x"),
                Component.literal("Each level adds another +10%"),
                Component.literal("Helps area and spread effects")
        ));
        drawLabeledModifier(graphics, new ItemStack(Items.LAPIS_LAZULI), "XP", x + 94, y + 33, mouseX, mouseY, List.of(
                Component.literal("Wisdom"),
                Component.literal("Level 1: spell XP 1.15x"),
                Component.literal("Each level adds another +15%"),
                Component.literal("No hard cap except wand slots")
        ));
        return y + 72;
    }

    private void drawGridSlot(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0x664B3420);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x33FFFFFF);
        if (!stack.isEmpty()) {
            graphics.renderFakeItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    private void drawItemBox(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 20, y + 20, 0x664B3420);
        graphics.fill(x - 1, y - 1, x + 19, y + 19, 0x33FFFFFF);
        graphics.renderFakeItem(stack, x + 1, y + 1);
        graphics.renderItemDecorations(font, stack, x + 1, y + 1);
    }

    private void drawLabeledItem(GuiGraphics graphics, ItemStack stack, String label, int x, int y) {
        drawItemBox(graphics, stack, x + 8, y);
        graphics.drawCenteredString(font, label, x + 18, y + 23, MUTED_INK);
    }

    private void drawLabeledModifier(GuiGraphics graphics, ItemStack stack, String label, int x, int y, int mouseX, int mouseY, List<Component> tooltip) {
        drawLabeledItem(graphics, stack, label, x, y);
        if (mouseX >= x + 6 && mouseX < x + 30 && mouseY >= y - 2 && mouseY < y + 31) {
            graphics.fill(x + 6, y - 2, x + 30, y + 31, 0x22FFFFFF);
            hoveredTooltip = tooltip;
        }
    }

    private void drawPlus(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, "+", x, y, GOLD, false);
    }

    private void drawArrow(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, ">", x, y, GOLD, false);
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

    private record Page(String title, String summary, List<String> lines, PageVisual visual) {
        private Page(String title, String summary, List<String> lines) {
            this(title, summary, lines, PageVisual.NONE);
        }
    }

    private enum PageVisual {
        NONE,
        WAND_SHAPELESS,
        WAND_GRID,
        WAND_COMBINE,
        WAND_MODIFIER,
        MODIFIER_ITEMS
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
