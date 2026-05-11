package com.ourmagic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.WandSelectSpellPacket;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WandHudOverlay {
    private static final int WIDTH = 144;
    private static final int BAR_HEIGHT = 7;
    private static final int PADDING = 5;
    private static final int SPELL_COLUMNS = 3;
    private static final int VISIBLE_ROWS = 4;
    private static final int ASSIGNED_SLOTS = 6;
    private static final int SLOT = 18;
    private static final int GAP = 2;
    private static final int HUD_X = 8;
    private static int spellScrollOffset;
    private static int selectedForAssignment = -1;

    private WandHudOverlay() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("wand_hud", WandHudOverlay::render);
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || ClientScryData.active()) {
            return;
        }

        ActiveWand active = activeWand(minecraft);
        if (active.stack().isEmpty()) {
            return;
        }

        WandData data = WandData.read(active.stack());
        int spellCost = data.activeManaCost();

        int x = activePanelX();
        int y = activePanelY(screenHeight);
        int panelHeight = 23;
        graphics.fill(x - PADDING, y - PADDING, x + WIDTH + PADDING, y + panelHeight, 0x99000000);

        int manaWidth = fillWidth(ClientManaData.mana(), ClientManaData.maxMana());
        graphics.drawString(minecraft.font, Component.literal(data.activeSpellName()), x, y, 0xFFE6D6FF, false);
        graphics.drawString(minecraft.font, Component.literal("Cost " + spellCost), x + WIDTH - 38, y, 0xFFC7E8FF, false);

        int manaY = y + 12;
        drawBar(graphics, x, manaY, WIDTH, BAR_HEIGHT, 0xFF172B45, 0xFF2D8CFF, manaWidth);
        graphics.drawString(minecraft.font, Component.literal("Mana " + ClientManaData.mana() + "/" + ClientManaData.maxMana() + "  +" + ClientManaData.regen() + "/s"), x + 3, manaY - 1, 0xFFFFFFFF, false);

        if (shiftDown(minecraft)) {
            renderSpellList(graphics, minecraft, data, screenWidth, screenHeight);
        }
        renderAssignedBar(graphics, minecraft, data, screenWidth, screenHeight);
        renderHoverTooltip(graphics, minecraft, data, screenWidth, screenHeight);
        renderHotbarCooldowns(graphics, minecraft, screenWidth, screenHeight);
        ClientFocusMode.render(graphics, minecraft, screenWidth, screenHeight);
        ClientChantMode.render(graphics, minecraft, screenWidth, screenHeight);
    }

    static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ActiveWand active = activeWand(minecraft);
        if (active.stack().isEmpty()) {
            return false;
        }

        WandData data = WandData.read(active.stack());
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        if (shiftDown(minecraft)) {
            int spellIndex = spellListIndexAt(mouseX, mouseY, data.spells().size(), screenWidth, screenHeight);
            if (spellIndex >= 0) {
                selectedForAssignment = spellIndex;
                send(active.hand(), WandSelectSpellPacket.Mode.SELECT_INDEX, 0, spellIndex);
                return true;
            }
        }

        int assignedSlot = assignedSlotAt(mouseX, mouseY, screenWidth, screenHeight);
        if (assignedSlot >= 0) {
            if (selectedForAssignment >= 0) {
                send(active.hand(), WandSelectSpellPacket.Mode.ASSIGN, assignedSlot, selectedForAssignment);
            } else {
                send(active.hand(), WandSelectSpellPacket.Mode.SELECT_ASSIGNED, assignedSlot, -1);
            }
            return true;
        }
        return false;
    }

    static boolean mouseScrolled(double delta) {
        Minecraft minecraft = Minecraft.getInstance();
        ActiveWand active = activeWand(minecraft);
        if (active.stack().isEmpty() || !shiftDown(minecraft)) {
            return false;
        }

        int spellCount = WandData.read(active.stack()).spells().size();
        int maxOffset = Math.max(0, rowsFor(spellCount) - VISIBLE_ROWS);
        int previous = spellScrollOffset;
        spellScrollOffset = Math.max(0, Math.min(maxOffset, spellScrollOffset - (int) Math.signum(delta)));
        return previous != spellScrollOffset;
    }

    static boolean selectAssignedSlot(int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        ActiveWand active = activeWand(minecraft);
        if (active.stack().isEmpty()) {
            return false;
        }
        if (selectedForAssignment >= 0) {
            send(active.hand(), WandSelectSpellPacket.Mode.ASSIGN, slot, selectedForAssignment);
            selectedForAssignment = -1;
            return true;
        }
        send(active.hand(), WandSelectSpellPacket.Mode.SELECT_ASSIGNED, slot, -1);
        return true;
    }

    private static ActiveWand activeWand(Minecraft minecraft) {
        if (minecraft.player == null) {
            return new ActiveWand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (isWand(mainHand)) {
            return new ActiveWand(InteractionHand.MAIN_HAND, mainHand);
        }
        ItemStack offHand = minecraft.player.getOffhandItem();
        return isWand(offHand) ? new ActiveWand(InteractionHand.OFF_HAND, offHand) : new ActiveWand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static void send(InteractionHand hand, WandSelectSpellPacket.Mode mode, int slot, int spellIndex) {
        ModNetwork.CHANNEL.sendToServer(new WandSelectSpellPacket(hand, mode, slot, spellIndex));
    }

    private static void renderSpellList(GuiGraphics graphics, Minecraft minecraft, WandData data, int screenWidth, int screenHeight) {
        List<WandData.WandSpellData> spells = data.spells();
        int rows = VISIBLE_ROWS;
        int width = SPELL_COLUMNS * SLOT + (SPELL_COLUMNS - 1) * GAP;
        int height = rows * SLOT + (rows - 1) * GAP;
        int leftX = spellListX(screenWidth);
        int leftY = spellListY(screenHeight);
        graphics.fill(leftX - 4, leftY - 15, leftX + width + 4, leftY + height + 4, 0x99000000);
        graphics.drawString(minecraft.font, Component.literal("Spells"), leftX, leftY - 12, 0xFFE6D6FF, false);

        int maxOffset = Math.max(0, rowsFor(spells.size()) - VISIBLE_ROWS);
        spellScrollOffset = Math.max(0, Math.min(spellScrollOffset, maxOffset));
        int firstIndex = spellScrollOffset * SPELL_COLUMNS;
        for (int i = 0; i < SPELL_COLUMNS * VISIBLE_ROWS; i++) {
            int spellIndex = firstIndex + i;
            int col = i % SPELL_COLUMNS;
            int row = i / SPELL_COLUMNS;
            int x = leftX + col * (SLOT + GAP);
            int y = leftY + row * (SLOT + GAP);
            drawSpellSlot(graphics, minecraft, spells, data.activeIndex(), spellIndex, x, y, spellIndex == selectedForAssignment);
        }
    }

    private static void renderAssignedBar(GuiGraphics graphics, Minecraft minecraft, WandData data, int screenWidth, int screenHeight) {
        List<WandData.WandSpellData> spells = data.spells();
        List<Integer> assigned = data.assignedSpells();
        int totalWidth = ASSIGNED_SLOTS * SLOT + (ASSIGNED_SLOTS - 1) * GAP;
        int startX = assignedBarX();
        int y = assignedBarY(screenHeight);

        graphics.fill(startX - 4, y - 4, startX + totalWidth + 4, y + SLOT + 14, 0x99000000);
        for (int slot = 0; slot < ASSIGNED_SLOTS; slot++) {
            int spellIndex = slot < assigned.size() ? assigned.get(slot) : 0;
            int x = startX + slot * (SLOT + GAP);
            drawSpellSlot(graphics, minecraft, spells, data.activeIndex(), spellIndex, x, y, false);
            String key = String.valueOf(slot + 1);
            graphics.drawString(minecraft.font, Component.literal(key), x + 7, y + SLOT + 2, 0xFFFFFFFF, false);
        }
    }

    private static void drawSpellSlot(GuiGraphics graphics, Minecraft minecraft, List<WandData.WandSpellData> spells, int activeIndex, int spellIndex, int x, int y, boolean selected) {
        int border = spellIndex == activeIndex ? 0xFF66FFAA : selected ? 0xFFFFD84D : 0xFF6A6078;
        graphics.fill(x, y, x + SLOT, y + SLOT, 0xCC101018);
        graphics.fill(x, y, x + SLOT, y + 1, border);
        graphics.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, border);
        graphics.fill(x, y, x + 1, y + SLOT, border);
        graphics.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, border);
        if (spellIndex < 0 || spellIndex >= spells.size()) {
            return;
        }

        WandData.WandSpellData spell = spells.get(spellIndex);
        int color = iconColor(spell.key());
        graphics.fill(x + 3, y + 3, x + SLOT - 3, y + SLOT - 3, color);
        String letter = spell.displayName().isBlank() ? "?" : spell.displayName().substring(0, 1);
        graphics.drawString(minecraft.font, Component.literal(letter), x + 6, y + 5, 0xFFFFFFFF, true);
    }

    private static void renderHoverTooltip(GuiGraphics graphics, Minecraft minecraft, WandData data, int screenWidth, int screenHeight) {
        double mouseX = minecraft.mouseHandler.xpos() * screenWidth / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * screenHeight / minecraft.getWindow().getScreenHeight();
        int spellIndex = shiftDown(minecraft) ? spellListIndexAt(mouseX, mouseY, data.spells().size(), screenWidth, screenHeight) : -1;
        if (spellIndex < 0) {
            int assignedSlot = assignedSlotAt(mouseX, mouseY, screenWidth, screenHeight);
            if (assignedSlot >= 0 && assignedSlot < data.assignedSpells().size()) {
                spellIndex = data.assignedSpells().get(assignedSlot);
            }
        }
        if (spellIndex < 0 || spellIndex >= data.spells().size()) {
            return;
        }

        WandData.WandSpellData spell = data.spells().get(spellIndex);
        Spell registeredSpell = SpellRegistry.get(spell.key());
        String focusText = registeredSpell == null ? "Unknown" : titleCase(registeredSpell.focusEffect().name());
        graphics.renderTooltip(minecraft.font, List.of(
                Component.literal(spell.displayName()),
                Component.literal(spell.key()),
                Component.literal("Mana " + spell.manaCost() + " | Cooldown " + String.format("%.1fs", spell.cooldownTicks() / 20.0F)),
                Component.literal("Level " + spell.level() + " | Points " + spell.attributePoints()),
                Component.literal("Focus: " + focusText)
        ), java.util.Optional.empty(), (int) mouseX, (int) mouseY);
    }

    private static String titleCase(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return lower.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + lower.substring(1);
    }

    private static int spellListIndexAt(double mouseX, double mouseY, int spellCount, int screenWidth, int screenHeight) {
        int width = SPELL_COLUMNS * SLOT + (SPELL_COLUMNS - 1) * GAP;
        int height = VISIBLE_ROWS * SLOT + (VISIBLE_ROWS - 1) * GAP;
        int leftX = spellListX(screenWidth);
        int leftY = spellListY(screenHeight);
        if (mouseX < leftX || mouseX >= leftX + width || mouseY < leftY || mouseY >= leftY + height) {
            return -1;
        }
        int col = (int) ((mouseX - leftX) / (SLOT + GAP));
        int row = (int) ((mouseY - leftY) / (SLOT + GAP));
        if (col < 0 || col >= SPELL_COLUMNS || row < 0 || row >= VISIBLE_ROWS) {
            return -1;
        }
        int index = (spellScrollOffset + row) * SPELL_COLUMNS + col;
        return index >= 0 && index < spellCount ? index : -1;
    }

    private static int assignedSlotAt(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int startX = assignedBarX();
        int y = assignedBarY(screenHeight);
        if (mouseY < y || mouseY >= y + SLOT) {
            return -1;
        }
        int slot = (int) ((mouseX - startX) / (SLOT + GAP));
        int slotX = startX + slot * (SLOT + GAP);
        return slot >= 0 && slot < ASSIGNED_SLOTS && mouseX >= slotX && mouseX < slotX + SLOT ? slot : -1;
    }

    private static int spellListX(int screenWidth) {
        return HUD_X;
    }

    private static int spellListY(int screenHeight) {
        return screenHeight - 144;
    }

    private static int activePanelX() {
        return (Minecraft.getInstance().getWindow().getGuiScaledWidth() - WIDTH) / 2;
    }

    private static int activePanelY(int screenHeight) {
        return screenHeight - 72;
    }

    private static int assignedBarX() {
        return HUD_X;
    }

    private static int assignedBarY(int screenHeight) {
        return screenHeight - 36;
    }

    private static boolean shiftDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static int rowsFor(int spellCount) {
        return Math.max(1, (spellCount + SPELL_COLUMNS - 1) / SPELL_COLUMNS);
    }

    private static int iconColor(String key) {
        int hash = key.hashCode();
        int r = 80 + Math.abs(hash & 0x7F);
        int g = 80 + Math.abs(hash >> 8 & 0x7F);
        int b = 80 + Math.abs(hash >> 16 & 0x7F);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int fillWidth(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(WIDTH, Math.round(WIDTH * (value / (float) max))));
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, int background, int foreground, int fillWidth) {
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + fillWidth, y + height, foreground);
        graphics.fill(x, y, x + width, y + 1, 0x66FFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xAA000000);
    }

    private static void renderHotbarCooldowns(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight) {
        Inventory inventory = minecraft.player.getInventory();
        long gameTime = minecraft.level.getGameTime();
        int hotbarLeft = screenWidth / 2 - 91;
        int hotbarTop = screenHeight - 22;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isWand(stack)) {
                continue;
            }

            WandData data = WandData.read(stack);
            int cooldownTicks = data.activeCooldownTicks();
            if (cooldownTicks <= 0) {
                continue;
            }

            long remaining = data.cooldownUntil() - gameTime;
            if (remaining <= 0) {
                continue;
            }

            float progress = Math.max(0.0F, Math.min(1.0F, remaining / (float) cooldownTicks));
            int alpha = 70 + Math.round(145 * progress);
            int green = Math.round(48 * (1.0F - progress));
            int overlayColor = (alpha << 24) | (0xFF << 16) | (green << 8);
            int slotX = hotbarLeft + slot * 20 + 2;
            int slotY = hotbarTop + 2;
            int cooldownHeight = Math.max(1, Math.round(16 * progress));

            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, overlayColor);
            graphics.fill(slotX, slotY + 16 - cooldownHeight, slotX + 16, slotY + 16, 0x66FF0000);

            String seconds = remaining >= 20 ? String.valueOf((int) Math.ceil(remaining / 20.0F)) : String.format("%.1f", remaining / 20.0F);
            int textX = slotX + 8 - minecraft.font.width(seconds) / 2;
            int textY = slotY + 4;
            graphics.drawString(minecraft.font, seconds, textX + 1, textY + 1, 0xCC000000, false);
            graphics.drawString(minecraft.font, seconds, textX, textY, 0xFFFFFFFF, false);
        }
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private record ActiveWand(InteractionHand hand, ItemStack stack) {
    }
}
