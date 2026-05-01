package com.ourmagic.client;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.UpgradeSpellPacket;
import com.ourmagic.ui.WandMenu;
import com.ourmagic.wand.WandData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class WandScreen extends AbstractContainerScreen<WandMenu> {
    private static final int WIDTH = 326;
    private static final int HEIGHT = 236;
    private static final int SPELL_SLOT_COUNT = 7;
    private static final int SPELL_BUTTON_X = 18;
    private static final int SPELL_BUTTON_Y = 42;
    private static final int SPELL_BUTTON_WIDTH = 128;
    private static final int SPELL_BUTTON_HEIGHT = 22;
    private static final int SPELL_BUTTON_GAP = 7;
    private static final int DETAIL_X = 174;
    private static final int SPELL_UPGRADE_BUTTON_X = 252;
    private static final int SPELL_UPGRADE_BUTTON_WIDTH = 52;
    private static final int UPGRADE_BUTTON_X = 16;
    private static final int UPGRADE_BUTTON_Y = 207;
    private static final int UPGRADE_BUTTON_WIDTH = 94;
    private static final int UPGRADE_BUTTON_HEIGHT = 18;
    private static final int UPGRADE_BUTTON_GAP_X = 8;
    private static final int UPGRADE_BUTTON_GAP_Y = 0;
    private static final int UPGRADE_BUTTON_COLUMNS = 3;
    private static final int COLOR_FRAME = 0xFF6F4A96;
    private static final int COLOR_PANEL = 0xAA080A12;
    private static final int COLOR_LINE = 0x884A5268;
    private static final int COLOR_CYAN = 0xFF55E8FF;
    private static final int COLOR_GREEN = 0xFF67FF61;
    private static final int COLOR_GOLD = 0xFFFFD84A;
    private static final int COLOR_ORANGE = 0xFFFF9C25;
    private static final int COLOR_PURPLE = 0xFFC66CFF;

    private int selectedSpell;
    private String selectedUpgrade;
    private View view = View.SPELL_DETAILS;

    private final List<Button> spellButtons = new ArrayList<>();
    private final List<Button> upgradeButtons = new ArrayList<>();

    private Button mainUpgradeButton;
    private Button backButton;
    private final List<Button> statButtons = new ArrayList<>();

    private enum View {
        SPELL_DETAILS,
        UPGRADE_DETAILS
    }

    public WandScreen(WandMenu menu, Inventory inventory, Component title) {
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
        spellButtons.clear();
        upgradeButtons.clear();
        statButtons.clear();

        WandData data = menu.wandData();
        List<WandData.WandSpellData> spells = data.spells();

        selectedSpell = Math.max(0, Math.min(selectedSpell, Math.max(0, spells.size() - 1)));

        int count = Math.min(spells.size(), SPELL_SLOT_COUNT);

        for (int i = 0; i < count; i++) {
            int spellIndex = i;
            WandData.WandSpellData spell = spells.get(i);

            Button button = addRenderableWidget(Button.builder(Component.literal(titleCase(spell.key())), b -> {
                        selectedSpell = spellIndex;
                        selectedUpgrade = null;
                        view = View.SPELL_DETAILS;
                    })
                    .bounds(leftPos + SPELL_BUTTON_X, topPos + SPELL_BUTTON_Y + i * (SPELL_BUTTON_HEIGHT + SPELL_BUTTON_GAP), SPELL_BUTTON_WIDTH, SPELL_BUTTON_HEIGHT)
                    .build());

            spellButtons.add(button);
        }

        for (int i = 0; i < 4; i++) {
            addUpgradeButton(i);
        }

        backButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                    selectedUpgrade = null;
                    view = View.SPELL_DETAILS;
                })
                .bounds(leftPos + 15, topPos + 14, 24, 20)
                .build());

        mainUpgradeButton = addRenderableWidget(Button.builder(Component.literal("Enable"), b -> sendUpgrade(selectedUpgrade))
                .bounds(leftPos + 102, topPos + 207, 122, UPGRADE_BUTTON_HEIGHT)
                .build());

        addStatButton(0);
        addStatButton(1);
        addStatButton(2);
    }

    private void addUpgradeButton(int index) {
        Button button = addRenderableWidget(Button.builder(yellow("Upgrade"), b -> {
                    List<String> upgrades = supportedUpgradeKeys(currentRegisteredSpell());
                    if (index < upgrades.size()) {
                        openUpgradeView(upgrades.get(index));
                    }
                })
                .bounds(buttonX(index), buttonY(index), UPGRADE_BUTTON_WIDTH, UPGRADE_BUTTON_HEIGHT)
                .build());
        upgradeButtons.add(button);
    }

    private void addStatButton(int index) {
        Button button = addRenderableWidget(Button.builder(yellow("Stat"), b -> {
                    String statUpgrade = statUpgradeKey(index);
                    if (statUpgrade != null) {
                        sendUpgrade(statUpgrade);
                    }
                })
                .bounds(buttonX(index), buttonY(index), UPGRADE_BUTTON_WIDTH, UPGRADE_BUTTON_HEIGHT)
                .build());
        statButtons.add(button);
    }

    private void openUpgradeView(String upgrade) {
        selectedUpgrade = upgrade;
        view = View.UPGRADE_DETAILS;
    }

    private void sendUpgrade(String upgrade) {
        if (upgrade != null) {
            ModNetwork.CHANNEL.sendToServer(new UpgradeSpellPacket(selectedSpell, upgrade));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE17141F);
        drawBorder(graphics, leftPos, topPos, imageWidth, imageHeight, COLOR_FRAME);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 2, 0xFF17101F);

        if (view == View.SPELL_DETAILS) {
            graphics.fill(leftPos + 12, topPos + 32, leftPos + 154, topPos + imageHeight - 12, COLOR_PANEL);
            graphics.fill(leftPos + 166, topPos + 32, leftPos + imageWidth - 14, topPos + imageHeight - 12, COLOR_PANEL);
            graphics.fill(leftPos + 178, topPos + 78, leftPos + imageWidth - 28, topPos + 79, COLOR_LINE);
            graphics.fill(leftPos + 178, topPos + 151, leftPos + imageWidth - 28, topPos + 152, COLOR_LINE);
        } else {
            graphics.fill(leftPos + 12, topPos + 12, leftPos + imageWidth - 12, topPos + imageHeight - 12, COLOR_PANEL);
            graphics.fill(leftPos + 96, topPos + 29, leftPos + imageWidth - 72, topPos + 30, 0xAA9E46D8);
            graphics.fill(leftPos + 16, topPos + 112, leftPos + imageWidth - 16, topPos + 113, COLOR_LINE);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        WandData data = menu.wandData();
        List<WandData.WandSpellData> spells = data.spells();

        if (spells.isEmpty()) {
            hideAllButtons();
            graphics.drawString(font, "No spells stored", 16, 24, 0xFFFF6666, false);
            return;
        }

        selectedSpell = Math.max(0, Math.min(selectedSpell, spells.size() - 1));

        WandData.WandSpellData spell = spells.get(selectedSpell);
        Spell registeredSpell = SpellRegistry.get(spell.key());

        if (view == View.UPGRADE_DETAILS && selectedUpgrade != null && !supports(registeredSpell, selectedUpgrade)) {
            selectedUpgrade = null;
            view = View.SPELL_DETAILS;
        }

        if (view == View.SPELL_DETAILS) {
            renderSpellView(graphics, data, spell, registeredSpell);
        } else {
            renderUpgradeView(graphics, spell);
        }
    }

    private void renderSpellView(GuiGraphics graphics, WandData data, WandData.WandSpellData spell, Spell registeredSpell) {
        showSpellViewButtons(spell, registeredSpell);

        graphics.drawString(font, data.displayName(), 14, 13, 0xFFFFFFFF, false);
        graphics.drawCenteredString(font, "*", imageWidth / 2, 17, COLOR_PURPLE);
        graphics.drawString(font, "Spells", 18, 32, COLOR_CYAN, false);
        renderSpellSlots(graphics);

        int x = DETAIL_X;
        int y = 47;

        graphics.drawString(font, titleCase(spell.key()), x, y, 0xFFFFFFFF, false);
        y += 16;

        graphics.drawString(font, "Level " + spell.level() + "/100", x, y, COLOR_GREEN, false);
        y += 12;

        if (spell.xpToNextLevel() > 0) {
            graphics.drawString(font, "XP " + spell.xp() + "/" + spell.xpToNextLevel(), x, y, 0xFF72D9FF, false);
        } else {
            graphics.drawString(font, "XP MAX", x, y, COLOR_GOLD, false);
        }

        y = 92;
        graphics.drawString(font, "* Points: " + spell.attributePoints(), x, y, COLOR_GOLD, false);

        y += 15;
        graphics.drawString(font, "* Mana: " + spell.manaCost(), x, y, 0xFF72D9FF, false);

        y += 15;
        graphics.drawString(font, "* Cooldown: " + String.format("%.1fs", spell.cooldownTicks() / 20.0F), x, y, COLOR_ORANGE, false);

        y += 15;
        graphics.drawString(font, "* Power: " + String.format("%.2fx", data.power()), x, y, COLOR_PURPLE, false);

        y = 164;
        graphics.drawString(font, "Upgrade Options", x, y, 0xFFFFFFFF, false);

        y += 15;

        if (supports(registeredSpell, Spell.UPGRADE_CHAINING)) {
            graphics.drawString(font, "Chaining: " + spell.chaining(), x, y, upgradeColor(Spell.UPGRADE_CHAINING), false);
            y += 13;
        }

        if (supports(registeredSpell, Spell.UPGRADE_MULTISTRIKE)) {
            graphics.drawString(font, "Multistrike: " + spell.multistrike(), x, y, upgradeColor(Spell.UPGRADE_MULTISTRIKE), false);
            y += 13;
        }

        if (supports(registeredSpell, Spell.UPGRADE_DAMAGE)) {
            graphics.drawString(font, "Damage: " + spell.damage(), x, y, upgradeColor(Spell.UPGRADE_DAMAGE), false);
            y += 13;
        }

        if (supports(registeredSpell, Spell.UPGRADE_RANGE)) {
            graphics.drawString(font, "Range: " + spell.range(), x, y, upgradeColor(Spell.UPGRADE_RANGE), false);
            y += 13;
        }

        if (supports(registeredSpell, Spell.UPGRADE_RADIUS)) {
            graphics.drawString(font, "Radius: " + spell.radius(), x, y, upgradeColor(Spell.UPGRADE_RADIUS), false);
            y += 13;
        }

        if (supports(registeredSpell, Spell.UPGRADE_DURATION)) {
            graphics.drawString(font, "Duration: " + spell.duration(), x, y, upgradeColor(Spell.UPGRADE_DURATION), false);
        }
    }

    private void renderUpgradeView(GuiGraphics graphics, WandData.WandSpellData spell) {
        showUpgradeViewButtons(spell);

        int rank = getUpgradeRank(spell, selectedUpgrade);
        int nextRank = rank + 1;

        int x = 126;
        int y = 17;

        graphics.drawCenteredString(font, titleCase(selectedUpgrade), imageWidth / 2, y, 0xFFFFFFFF);

        renderUpgradeIcon(graphics, selectedUpgrade, rank);

        y = 58;
        graphics.drawString(font, upgradeDescription(selectedUpgrade), x, y, 0xFFB7B7C4, false);

        renderStatCards(graphics, spell, rank, nextRank);

        String cost = rank <= 0 ? "Cost: 1 Point" : "Cost: 1 Point | Your Points: " + spell.attributePoints();
        graphics.drawCenteredString(font, cost, imageWidth / 2, 191, COLOR_GOLD);
    }

    private void renderSpellSlots(GuiGraphics graphics) {
        List<WandData.WandSpellData> spells = menu.wandData().spells();
        int count = Math.min(spells.size(), SPELL_SLOT_COUNT);
        for (int i = 0; i < count; i++) {
            int x = SPELL_BUTTON_X;
            int y = SPELL_BUTTON_Y + i * (SPELL_BUTTON_HEIGHT + SPELL_BUTTON_GAP);
            int border = i == selectedSpell ? 0xFFB45CFF : 0xFF343846;

            graphics.fill(x, y, x + SPELL_BUTTON_WIDTH, y + SPELL_BUTTON_HEIGHT, 0xAA11131B);
            drawBorder(graphics, x, y, SPELL_BUTTON_WIDTH, SPELL_BUTTON_HEIGHT, border);
        }
    }

    private void renderUpgradeIcon(GuiGraphics graphics, String upgrade, int rank) {
        int x = 42;
        int y = 45;
        int color = upgradeColor(upgrade);
        graphics.fill(x - 13, y - 13, x + 45, y + 45, 0x66101822);
        drawBorder(graphics, x - 13, y - 13, 58, 58, 0x66408DA0);

        switch (upgrade) {
            case Spell.UPGRADE_CHAINING -> {
                graphics.drawString(font, "[]", x + 2, y + 4, color, false);
                graphics.drawString(font, "  []", x + 9, y + 13, color, false);
            }
            case Spell.UPGRADE_MULTISTRIKE -> {
                graphics.drawString(font, "***", x + 2, y + 7, color, false);
                graphics.drawString(font, " ***", x + 6, y + 16, color, false);
            }
            case Spell.UPGRADE_RANGE -> {
                graphics.drawString(font, "(o)", x + 3, y + 4, color, false);
                graphics.drawString(font, " /|\\", x, y + 14, color, false);
            }
            case Spell.UPGRADE_DURATION -> {
                graphics.drawString(font, "O", x + 10, y + 2, color, false);
                graphics.drawString(font, "| |", x + 4, y + 13, color, false);
            }
            default -> graphics.drawString(font, "++", x + 8, y + 10, color, false);
        }

        if (rank > 0) {
            graphics.drawCenteredString(font, String.valueOf(rank), x + 16, y + 31, color);
        }
    }

    private void renderStatCards(GuiGraphics graphics, WandData.WandSpellData spell, int rank, int nextRank) {
        for (int i = 0; i < 3; i++) {
            String upgrade = statUpgradeKey(i);
            if (upgrade == null) {
                continue;
            }

            int x = 16 + i * 102;
            int y = 122;
            int color = statColor(upgrade);
            graphics.fill(x, y, x + 94, y + 62, 0x8810131E);
            drawBorder(graphics, x, y, 94, 62, 0xFF2A3040);
            graphics.drawCenteredString(font, statIcon(upgrade), x + 47, y + 9, color);
            graphics.drawCenteredString(font, cardLabel(upgrade), x + 47, y + 24, color);
            graphics.drawCenteredString(font, statValue(spell, upgrade, rank, nextRank), x + 47, y + 36, 0xFFFFFFFF);
            graphics.drawCenteredString(font, statDescription(upgrade), x + 47, y + 50, 0xFFB7B7C4);
        }
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void showSpellViewButtons(WandData.WandSpellData spellData, Spell registeredSpell) {
        for (Button button : spellButtons) {
            button.visible = true;
            button.active = true;
        }

        updateUpgradeButtons(spellData, registeredSpell);

        if (backButton != null) backButton.visible = false;
        if (mainUpgradeButton != null) mainUpgradeButton.visible = false;
        for (Button button : statButtons) {
            button.visible = false;
        }
    }

    private void showUpgradeViewButtons(WandData.WandSpellData spell) {
        for (Button button : spellButtons) {
            button.visible = false;
        }

        for (Button button : upgradeButtons) {
            button.visible = false;
        }
        for (Button button : statButtons) {
            button.visible = false;
        }

        int rank = getUpgradeRank(spell, selectedUpgrade);
        boolean canUpgrade = selectedUpgrade != null && spell.canUpgrade(selectedUpgrade);

        if (backButton != null) {
            backButton.visible = true;
            backButton.active = true;
        }

        if (mainUpgradeButton != null) {
            mainUpgradeButton.visible = rank <= 0;
            mainUpgradeButton.active = canUpgrade;
            mainUpgradeButton.setX(leftPos + 102);
            mainUpgradeButton.setY(topPos + 207);
            mainUpgradeButton.setMessage(yellow(rank <= 0
                    ? "Enable " + titleCase(selectedUpgrade)
                    : "Upgrade"));
        }

        updateStatButtons(spell, rank);
    }

    private void hideAllButtons() {
        for (Button button : spellButtons) {
            button.visible = false;
        }

        for (Button button : upgradeButtons) {
            button.visible = false;
        }
        if (backButton != null) backButton.visible = false;
        if (mainUpgradeButton != null) mainUpgradeButton.visible = false;
        for (Button button : statButtons) {
            button.visible = false;
        }
    }

    private void updateUpgradeButtons(WandData.WandSpellData spellData, Spell spell) {
        List<String> upgrades = supportedUpgradeKeys(spell);
        for (int i = 0; i < upgradeButtons.size(); i++) {
            Button button = upgradeButtons.get(i);
            if (i >= upgrades.size()) {
                button.visible = false;
                continue;
            }

            String upgrade = upgrades.get(i);
            String label = upgradeLabel(upgrade);
            button.setX(leftPos + SPELL_UPGRADE_BUTTON_X);
            button.setY(topPos + 175 + i * 13);
            button.setWidth(SPELL_UPGRADE_BUTTON_WIDTH);
            button.setMessage(yellow(getUpgradeRank(spellData, upgrade) <= 0 ? "Add" : "View"));
            button.visible = true;
            button.active = true;
            button.setTooltip(Tooltip.create(Component.literal(upgradeTooltip(spellData, upgrade))));
        }
    }

    private static int getUpgradeRank(WandData.WandSpellData spell, String upgrade) {
        if (upgrade == null) return 0;

        return switch (upgrade) {
            case Spell.UPGRADE_CHAINING -> spell.chaining();
            case Spell.UPGRADE_MULTISTRIKE -> spell.multistrike();
            case Spell.UPGRADE_DAMAGE -> spell.damage();
            case Spell.UPGRADE_RANGE -> spell.range();
            case Spell.UPGRADE_RADIUS -> spell.radius();
            case Spell.UPGRADE_DURATION -> spell.duration();
            default -> 0;
        };
    }

    private void updateStatButtons(WandData.WandSpellData spell, int rank) {
        for (int i = 0; i < statButtons.size(); i++) {
            Button button = statButtons.get(i);
            String upgrade = statUpgradeKey(i);
            if (rank <= 0 || upgrade == null) {
                button.visible = false;
                continue;
            }

            String label = statLabel(i);
            button.visible = true;
            button.active = spell.canUpgrade(upgrade);
            button.setX(buttonX(i));
            button.setY(buttonY(i));
            button.setWidth(UPGRADE_BUTTON_WIDTH);
            button.setMessage(Component.literal("Upgrade " + label).withStyle(colorStyle(upgrade)));
            button.setTooltip(Tooltip.create(Component.literal(statTooltip(upgrade))));
        }
    }

    private String statUpgradeKey(int index) {
        if (selectedUpgrade == null) {
            return null;
        }

        return switch (selectedUpgrade) {
            case Spell.UPGRADE_CHAINING -> switch (index) {
                case 0 -> Spell.UPGRADE_ENTITIES;
                default -> null;
            };
            case Spell.UPGRADE_MULTISTRIKE -> switch (index) {
                case 0 -> Spell.UPGRADE_CASTS;
                default -> null;
            };
            case Spell.UPGRADE_DAMAGE -> index == 0 ? Spell.UPGRADE_DAMAGE : null;
            case Spell.UPGRADE_RANGE -> index == 0 ? Spell.UPGRADE_RANGE : null;
            case Spell.UPGRADE_RADIUS -> index == 0 ? Spell.UPGRADE_RADIUS : null;
            case Spell.UPGRADE_DURATION -> index == 0 ? Spell.UPGRADE_DURATION : null;
            default -> null;
        };
    }

    private String statLabel(int index) {
        return switch (selectedUpgrade) {
            case Spell.UPGRADE_CHAINING -> switch (index) {
                case 0 -> "Entities";
                default -> "Stat";
            };
            case Spell.UPGRADE_MULTISTRIKE -> switch (index) {
                case 0 -> "Casts";
                default -> "Stat";
            };
            case Spell.UPGRADE_DAMAGE -> "Damage";
            case Spell.UPGRADE_RANGE -> "Reach";
            case Spell.UPGRADE_RADIUS -> "Radius";
            case Spell.UPGRADE_DURATION -> "Duration";
            default -> "Stat";
        };
    }

    private static String statTooltip(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_ENTITIES -> "Cost: 1 point, +mana";
            case Spell.UPGRADE_RADIUS -> "Cost: 1 point, +mana and +cooldown";
            case Spell.UPGRADE_CASTS -> "Cost: 1 point, +mana and +cooldown";
            case Spell.UPGRADE_DAMAGE -> "Cost: 1 point, +mana";
            case Spell.UPGRADE_RANGE -> "Cost: 1 point, +mana and +cooldown";
            case Spell.UPGRADE_DURATION -> "Cost: 1 point, +mana and +cooldown";
            default -> "Cost: 1 point";
        };
    }

    private static String cardLabel(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_ENTITIES -> "Targets";
            case Spell.UPGRADE_RADIUS -> "Radius";
            case Spell.UPGRADE_CASTS -> "Casts";
            case Spell.UPGRADE_DAMAGE -> "Damage";
            case Spell.UPGRADE_RANGE -> "Reach";
            case Spell.UPGRADE_DURATION -> "Duration";
            default -> titleCase(upgrade);
        };
    }

    private static String statIcon(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_ENTITIES, Spell.UPGRADE_CASTS -> "*";
            case Spell.UPGRADE_RADIUS, Spell.UPGRADE_RANGE -> "O";
            case Spell.UPGRADE_DAMAGE -> "+";
            case Spell.UPGRADE_DURATION -> "||";
            default -> "*";
        };
    }

    private static String statDescription(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_ENTITIES -> "Maximum targets hit.";
            case Spell.UPGRADE_RADIUS -> "Area and chain radius.";
            case Spell.UPGRADE_CASTS -> "Additional casts.";
            case Spell.UPGRADE_DAMAGE -> "Damage bonus.";
            case Spell.UPGRADE_RANGE -> "Spell reach.";
            case Spell.UPGRADE_DURATION -> "Effect time.";
            default -> "Upgrade stat.";
        };
    }

    private static String statValue(WandData.WandSpellData spell, String upgrade, int rank, int nextRank) {
        boolean showNext = rank > 0 && spell.canUpgrade(upgrade);
        return switch (upgrade) {
            case Spell.UPGRADE_ENTITIES -> arrowValue(1 + spell.chainingEntities(), showNext ? 2 + spell.chainingEntities() : null);
            case Spell.UPGRADE_RADIUS -> arrowValue("+" + (spell.radius() * 20) + "%", showNext ? "+" + ((spell.radius() + 1) * 20) + "%" : null);
            case Spell.UPGRADE_CASTS -> arrowValue(1 + rank + spell.multistrikeCasts(), showNext ? 2 + rank + spell.multistrikeCasts() : null);
            case Spell.UPGRADE_DAMAGE -> arrowValue("+" + (spell.damage() * 10) + "%", showNext ? "+" + ((spell.damage() + 1) * 10) + "%" : null);
            case Spell.UPGRADE_RANGE -> arrowValue("+" + (rank * 20) + "%", showNext ? "+" + (nextRank * 20) + "%" : null);
            case Spell.UPGRADE_DURATION -> arrowValue("+" + (rank * 20) + "%", showNext ? "+" + (nextRank * 20) + "%" : null);
            default -> "";
        };
    }

    private static String arrowValue(int current, Integer next) {
        return next == null ? String.valueOf(current) : current + " -> " + next;
    }

    private static String arrowValue(String current, String next) {
        return next == null ? current : current + " -> " + next;
    }

    private static String upgradeDescription(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_CHAINING -> "Chains to additional nearby targets.";
            case Spell.UPGRADE_MULTISTRIKE -> "Fires additional casts at the target.";
            case Spell.UPGRADE_DAMAGE -> "Increases spell damage.";
            case Spell.UPGRADE_RANGE -> "Extends how far the spell can reach.";
            case Spell.UPGRADE_RADIUS -> "Expands area and chain radius.";
            case Spell.UPGRADE_DURATION -> "Keeps spell effects active longer.";
            default -> "Improves this spell.";
        };
    }

    private static boolean supports(Spell spell, String upgrade) {
        return spell != null && spell.supportsUpgrade(upgrade);
    }

    private Spell currentRegisteredSpell() {
        List<WandData.WandSpellData> spells = menu.wandData().spells();
        if (selectedSpell < 0 || selectedSpell >= spells.size()) {
            return null;
        }
        return SpellRegistry.get(spells.get(selectedSpell).key());
    }

    private static List<String> supportedUpgradeKeys(Spell spell) {
        List<String> upgrades = new ArrayList<>();
        addIfSupported(upgrades, spell, Spell.UPGRADE_CHAINING);
        addIfSupported(upgrades, spell, Spell.UPGRADE_MULTISTRIKE);
        addIfSupported(upgrades, spell, Spell.UPGRADE_DAMAGE);
        addIfSupported(upgrades, spell, Spell.UPGRADE_RANGE);
        addIfSupported(upgrades, spell, Spell.UPGRADE_RADIUS);
        addIfSupported(upgrades, spell, Spell.UPGRADE_DURATION);
        return upgrades;
    }

    private static void addIfSupported(List<String> upgrades, Spell spell, String upgrade) {
        if (supports(spell, upgrade)) {
            upgrades.add(upgrade);
        }
    }

    private static String upgradeLabel(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_CHAINING -> "Chaining";
            case Spell.UPGRADE_MULTISTRIKE -> "Multi";
            case Spell.UPGRADE_DAMAGE -> "Damage";
            case Spell.UPGRADE_RANGE -> "Range";
            case Spell.UPGRADE_RADIUS -> "Radius";
            case Spell.UPGRADE_DURATION -> "Duration";
            default -> titleCase(upgrade);
        };
    }

    private static String upgradeTooltip(WandData.WandSpellData spell, String upgrade) {
        return titleCase(upgrade) + " " + getUpgradeRank(spell, upgrade) + " | Cost: 1 point";
    }

    private int buttonX(int index) {
        return leftPos + UPGRADE_BUTTON_X + (index % UPGRADE_BUTTON_COLUMNS) * (UPGRADE_BUTTON_WIDTH + UPGRADE_BUTTON_GAP_X);
    }

    private int buttonY(int index) {
        return topPos + UPGRADE_BUTTON_Y + (index / UPGRADE_BUTTON_COLUMNS) * (UPGRADE_BUTTON_HEIGHT + UPGRADE_BUTTON_GAP_Y);
    }

    private static Component yellow(String text) {
        return Component.literal(text).withStyle(ChatFormatting.YELLOW);
    }

    private static ChatFormatting colorStyle(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_CHAINING, Spell.UPGRADE_ENTITIES -> ChatFormatting.AQUA;
            case Spell.UPGRADE_RANGE, Spell.UPGRADE_RADIUS -> ChatFormatting.GREEN;
            case Spell.UPGRADE_DURATION -> ChatFormatting.YELLOW;
            case Spell.UPGRADE_MULTISTRIKE, Spell.UPGRADE_CASTS, Spell.UPGRADE_DAMAGE -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.YELLOW;
        };
    }

    private static int upgradeColor(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_CHAINING -> COLOR_CYAN;
            case Spell.UPGRADE_MULTISTRIKE -> COLOR_PURPLE;
            case Spell.UPGRADE_DAMAGE -> COLOR_PURPLE;
            case Spell.UPGRADE_RANGE -> COLOR_GREEN;
            case Spell.UPGRADE_RADIUS -> COLOR_GREEN;
            case Spell.UPGRADE_DURATION -> COLOR_GOLD;
            default -> COLOR_GOLD;
        };
    }

    private static int statColor(String upgrade) {
        return switch (upgrade) {
            case Spell.UPGRADE_ENTITIES -> COLOR_CYAN;
            case Spell.UPGRADE_RADIUS, Spell.UPGRADE_RANGE -> COLOR_GREEN;
            case Spell.UPGRADE_DAMAGE, Spell.UPGRADE_CASTS -> COLOR_PURPLE;
            case Spell.UPGRADE_DURATION -> COLOR_GOLD;
            default -> COLOR_GOLD;
        };
    }

    private static String titleCase(String value) {
        if (value == null || value.isEmpty()) {
            return "None";
        }

        String cleaned = value.replace("_", " ");
        return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }
}
