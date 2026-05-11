package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModItems;
import com.ourmagic.ui.PlayerUpgradeMenu;
import com.ourmagic.ui.SpellcraftMenu;
import com.ourmagic.ui.WandMenu;
import com.ourmagic.wand.WandData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class WandAnvilEvents {
    private static final int NORMAL_COMBINED_SPELL_LIMIT = 12;
    private static final int ADMIN_COMBINED_SPELL_LIMIT = 20;

    private WandAnvilEvents() {
    }

    @SubscribeEvent
    public static void updateAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!isWand(left) || !isWand(right)) {
            return;
        }

        WandData leftData = WandData.read(left);
        WandData rightData = WandData.read(right);
        int maxSpells = left.is(ModItems.ADMIN_WAND.get()) || right.is(ModItems.ADMIN_WAND.get())
                ? ADMIN_COMBINED_SPELL_LIMIT
                : NORMAL_COMBINED_SPELL_LIMIT;

        ItemStack output = left.copy();
        output.setCount(1);
        WandData.combine(leftData, rightData, maxSpells).save(output);
        event.setOutput(output);
        event.setCost(combineCost(leftData, rightData));
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isAnvil(event) && isWand(event.getEntity().getOffhandItem())) {
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                openPlayerUpgrades(event);
            } else {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            }
            return;
        }

        if (!isWand(event.getItemStack())) {
            return;
        }
        if (isEnchantingTable(event)) {
            openSpellcraft(event);
            return;
        }

        if (!isAnvil(event)) {
            return;
        }

        openWand(event);
    }

    private static void openPlayerUpgrades(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Player Upgrades");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new PlayerUpgradeMenu(containerId, inventory);
            }
        });
    }

    private static void openWand(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        InteractionHand hand = event.getHand();
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Wand");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new WandMenu(containerId, inventory, hand);
            }
        }, buffer -> buffer.writeEnum(hand));
    }

    private static void openSpellcraft(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        InteractionHand hand = event.getHand();
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Spellcraft");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new SpellcraftMenu(containerId, inventory, hand);
            }
        }, buffer -> buffer.writeEnum(hand));
    }

    private static boolean isEnchantingTable(PlayerInteractEvent.RightClickBlock event) {
        return event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE);
    }

    private static boolean isAnvil(PlayerInteractEvent.RightClickBlock event) {
        return event.getLevel().getBlockState(event.getPos()).is(Blocks.ANVIL)
                || event.getLevel().getBlockState(event.getPos()).is(Blocks.CHIPPED_ANVIL)
                || event.getLevel().getBlockState(event.getPos()).is(Blocks.DAMAGED_ANVIL);
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private static int combineCost(WandData left, WandData right) {
        int newSpellCount = 0;
        int duplicateCount = 0;
        int rightLevels = 0;

        for (WandData.WandSpellData rightSpell : right.spells()) {
            rightLevels += rightSpell.level();
            boolean duplicate = left.spells().stream().anyMatch(leftSpell -> leftSpell.key().equals(rightSpell.key()));
            if (duplicate) {
                duplicateCount++;
            } else {
                newSpellCount++;
            }
        }

        return Math.min(39, 6 + newSpellCount * 3 + duplicateCount * 2 + rightLevels / 25);
    }
}
