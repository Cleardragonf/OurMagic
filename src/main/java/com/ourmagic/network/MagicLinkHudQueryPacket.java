package com.ourmagic.network;

import com.ourmagic.block.entity.MagicAccumulatorBlockEntity;
import com.ourmagic.block.entity.MagicBatteryBlockEntity;
import com.ourmagic.block.entity.MagicFlowConverterBlockEntity;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record MagicLinkHudQueryPacket(BlockPos pos) {
    public static void encode(MagicLinkHudQueryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static MagicLinkHudQueryPacket decode(FriendlyByteBuf buffer) {
        return new MagicLinkHudQueryPacket(buffer.readBlockPos());
    }

    public static void handle(MagicLinkHudQueryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level) || !holdingLinker(player) || player.distanceToSqr(packet.pos.getCenter()) > 64.0D) {
                return;
            }

            HudData data = hudData(level, packet.pos);
            if (data == null) {
                return;
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MagicLinkHudDataPacket(packet.pos, data.title(), data.lines()));
        });
        context.setPacketHandled(true);
    }

    private static boolean holdingLinker(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ModItems.MAGIC_LINKER.get()) || off.is(ModItems.MAGIC_LINKER.get());
    }

    private static HudData hudData(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MagicAccumulatorBlockEntity accumulator) {
            return new HudData(accumulator.magicType().displayName() + " Accumulator", List.of(
                    "Stored: " + accumulator.stored() + "/" + accumulator.capacity() + " ME",
                    "Output: " + accumulator.lastPushed() + " ME/t",
                    "Mode: " + accumulator.transferMode().displayName(),
                    "Links: " + accumulator.linkCount() + "/" + accumulator.maxLinks()
            ));
        }
        if (blockEntity instanceof MagicBatteryBlockEntity) {
            MagicBatteryBlockEntity battery = MagicBatteryBlockEntity.getOrCreate(level, pos).orElse(null);
            if (battery == null) {
                return null;
            }
            List<String> lines = new ArrayList<>();
            lines.add("Blocks: " + battery.multiblockSize() + "  Cap/type: " + battery.capacity());
            for (MagicEnergyType type : MagicEnergyType.values()) {
                lines.add(type.displayName() + ": " + battery.stored(type) + " ME");
            }
            return new HudData("Magic Battery", lines);
        }
        if (blockEntity instanceof MagicFlowConverterBlockEntity converter) {
            return new HudData("Magic Flow Converter", List.of(
                    "Input: RF or Arcane ME",
                    "Output: Ward MF",
                    converter.linkedWardCoreSummary()
            ));
        }
        return null;
    }

    private record HudData(String title, List<String> lines) {
    }
}
