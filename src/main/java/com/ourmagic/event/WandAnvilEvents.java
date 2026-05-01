package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModItems;
import com.ourmagic.ui.WandMenu;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class WandAnvilEvents {
    private WandAnvilEvents() {
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!isAnvil(event) || !isWand(event.getItemStack())) {
            return;
        }

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

    private static boolean isAnvil(PlayerInteractEvent.RightClickBlock event) {
        return event.getLevel().getBlockState(event.getPos()).is(Blocks.ANVIL)
                || event.getLevel().getBlockState(event.getPos()).is(Blocks.CHIPPED_ANVIL)
                || event.getLevel().getBlockState(event.getPos()).is(Blocks.DAMAGED_ANVIL);
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }
}
