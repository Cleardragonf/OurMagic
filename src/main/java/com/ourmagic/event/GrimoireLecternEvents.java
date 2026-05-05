package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.item.GrimoireItem;
import com.ourmagic.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class GrimoireLecternEvents {
    private static final int CHARGE_SCAN_RADIUS = 8;
    private static final int CHARGE_PER_SECOND = 5;
    private static final String TAG_LAST_LECTERN_CHARGE_TICK = "OurMagicLastLecternChargeTick";

    private GrimoireLecternEvents() {
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.LECTERN)) {
            return;
        }

        ItemStack held = event.getItemStack();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (held.isEmpty() && state.getValue(LecternBlock.HAS_BOOK) && removeGrimoire(event, state)) {
            return;
        }

        if (!held.is(ModItems.GRIMOIRE.get())) {
            return;
        }

        if (state.getValue(LecternBlock.HAS_BOOK)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }

        ItemStack placementStack = event.getEntity().getAbilities().instabuild ? held.copy() : held;
        if (LecternBlock.tryPlaceBook(event.getEntity(), level, event.getPos(), state, placementStack)) {
            BlockEntity blockEntity = level.getBlockEntity(event.getPos());
            if (blockEntity instanceof LecternBlockEntity lectern) {
                lectern.setChanged();
            }
        }
    }

    private static boolean removeGrimoire(PlayerInteractEvent.RightClickBlock event, BlockState state) {
        Level level = event.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof LecternBlockEntity lectern) || !lectern.getBook().is(ModItems.GRIMOIRE.get())) {
            return false;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));

        if (level.isClientSide) {
            return true;
        }

        ItemStack book = lectern.getBook().copy();
        lectern.setBook(ItemStack.EMPTY);
        lectern.setChanged();
        LecternBlock.resetBookState(event.getEntity(), level, event.getPos(), state, false);

        if (!event.getEntity().getInventory().add(book)) {
            event.getEntity().drop(book, false);
        }
        return true;
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-CHARGE_SCAN_RADIUS, -4, -CHARGE_SCAN_RADIUS);
        BlockPos max = center.offset(CHARGE_SCAN_RADIUS, 4, CHARGE_SCAN_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(pos).is(Blocks.LECTERN)) {
                continue;
            }
            chargeLectern(level, pos.immutable());
        }
    }

    private static void chargeLectern(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof LecternBlockEntity lectern)) {
            return;
        }

        ItemStack book = lectern.getBook();
        if (!book.is(ModItems.GRIMOIRE.get())) {
            return;
        }

        long gameTime = level.getGameTime();
        if (book.getOrCreateTag().getLong(TAG_LAST_LECTERN_CHARGE_TICK) == gameTime) {
            return;
        }
        book.getOrCreateTag().putLong(TAG_LAST_LECTERN_CHARGE_TICK, gameTime);

        if (!GrimoireItem.addMagicCharge(book, CHARGE_PER_SECOND)) {
            return;
        }

        lectern.setChanged();
        Vec3 bookCenter = Vec3.atCenterOf(pos).add(0.0D, 0.45D, 0.0D);
        level.sendParticles(ParticleTypes.ENCHANT, bookCenter.x, bookCenter.y + 0.15D, bookCenter.z, 12, 0.85D, 0.55D, 0.85D, 0.0D);
        level.sendParticles(ParticleTypes.WITCH, bookCenter.x, bookCenter.y, bookCenter.z, 2, 0.12D, 0.08D, 0.12D, 0.0D);
    }
}
