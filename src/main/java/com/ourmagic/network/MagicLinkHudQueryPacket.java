package com.ourmagic.network;

import com.ourmagic.block.entity.MagicAccumulatorBlockEntity;
import com.ourmagic.block.entity.MagicBatteryBlockEntity;
import com.ourmagic.block.entity.MagicFlowConverterBlockEntity;
import com.ourmagic.block.entity.MagicRelayBlockEntity;
import com.ourmagic.item.WardTunerItem;
import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.magic.ward.WorldWards;
import com.ourmagic.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            HudTool tool = activeHudTool(player);
            if (tool == HudTool.NONE || player.distanceToSqr(packet.pos.getCenter()) > 64.0D) {
                return;
            }

            HudData data = hudData(level, player, packet.pos, tool);
            if (data == null) {
                return;
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MagicLinkHudDataPacket(packet.pos, data.title(), data.lines()));
        });
        context.setPacketHandled(true);
    }

    private static HudTool activeHudTool(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.is(ModItems.MAGIC_LINKER.get())) {
            return HudTool.MAGIC_LINKER;
        }
        if (main.is(ModItems.WARD_TUNER.get())) {
            return HudTool.WARD_TUNER;
        }
        if (off.is(ModItems.MAGIC_LINKER.get())) {
            return HudTool.MAGIC_LINKER;
        }
        if (off.is(ModItems.WARD_TUNER.get())) {
            return HudTool.WARD_TUNER;
        }
        return HudTool.NONE;
    }

    private static HudData hudData(ServerLevel level, ServerPlayer player, BlockPos pos, HudTool tool) {
        if (tool == HudTool.WARD_TUNER) {
            Optional<ItemStack> wardTuner = heldWardTuner(player);
            Optional<BlockPos> selectedAnchor = WardTunerItem.hasBoundWardStone(wardTuner.get()) && WardTunerItem.isBoundToDimension(wardTuner.get(), level)
                    ? Optional.of(WardTunerItem.boundWardStone(wardTuner.get()))
                    : Optional.empty();
            Optional<WorldWards.HudSummary> summary = WorldWards.hudSummary(level, pos, selectedAnchor);
            if (summary.isPresent()) {
                return new HudData(summary.get().title(), summary.get().lines());
            }
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (tool == HudTool.MAGIC_LINKER) {
            Optional<WorldWards.HudSummary> wardMfSummary = WorldWards.magicFlowHudSummary(level, pos);
            if (wardMfSummary.isPresent()) {
                return new HudData(wardMfSummary.get().title(), wardMfSummary.get().lines());
            }
        }
        if (blockEntity instanceof MagicAccumulatorBlockEntity accumulator) {
            return new HudData(accumulator.magicType().displayName() + " Accumulator", List.of(
                    progressLine("Stored", accumulator.stored(), accumulator.capacity()),
                    "Output: " + accumulator.lastPushed() + " ME/t",
                    "Mode: " + accumulator.transferMode().displayName(),
                    linkLine(accumulator.linkCount(), accumulator.inboundLinkCount(), accumulator.maxLinks())
            ));
        }
        if (blockEntity instanceof MagicBatteryBlockEntity) {
            MagicBatteryBlockEntity battery = MagicBatteryBlockEntity.getOrCreate(level, pos).orElse(null);
            if (battery == null) {
                return null;
            }
            List<String> lines = new ArrayList<>();
            lines.add("Blocks: " + battery.multiblockSize() + "  Cap/type: " + battery.capacity());
            lines.add(linkLine(battery.linkCount(), battery.inboundLinkCount(), battery.maxLinks()));
            for (MagicEnergyType type : MagicEnergyType.values()) {
                lines.add(progressLine(type.displayName(), battery.stored(type), battery.capacity()));
            }
            return new HudData("Magic Battery", lines);
        }
        if (blockEntity instanceof MagicRelayBlockEntity relay) {
            List<String> lines = new ArrayList<>();
            lines.add("Output: " + relay.lastPushed() + " ME/t");
            lines.add(linkLine(relay.linkCount(), relay.inboundLinkCount(), relay.maxLinks()));
            for (MagicEnergyType type : MagicEnergyType.values()) {
                lines.add(progressLine(type.displayName(), relay.stored(type), relay.capacity()));
            }
            return new HudData("Magic Relay", lines);
        }
        if (blockEntity instanceof MagicFlowConverterBlockEntity converter) {
            return new HudData("Magic Flow Converter", List.of(
                    "Input: RF or Arcane ME",
                    "Output: Ward MF",
                    converter.linkedWardCoreSummary()
            ));
        }
        if (blockEntity instanceof MagicEnergyReceiver receiver) {
            List<String> lines = new ArrayList<>();
            for (MagicEnergyType type : MagicEnergyType.values()) {
                int accepted = receiver.receiveMagicEnergy(type, 1_000_000, true);
                if (accepted > 0) {
                    lines.add(type.displayName() + " input space: " + accepted + " ME");
                }
            }
            if (!lines.isEmpty()) {
                return new HudData("Magic Receiver", lines);
            }
        }
        HudData energyCapabilityData = energyCapabilityData(blockEntity);
        if (energyCapabilityData != null) {
            return energyCapabilityData;
        }
        return null;
    }

    private static Optional<ItemStack> heldWardTuner(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.WARD_TUNER.get())) {
            return Optional.of(main);
        }
        ItemStack off = player.getOffhandItem();
        return off.is(ModItems.WARD_TUNER.get()) ? Optional.of(off) : Optional.empty();
    }

    private static String progressLine(String label, int value, int max) {
        return "@bar|" + label + "|" + Math.max(0, value) + "|" + Math.max(1, max);
    }

    private static String linkLine(int outgoing, int incoming, int max) {
        return "Links: " + (outgoing + incoming) + "/" + max + " (in " + incoming + ", out " + outgoing + ")";
    }

    private static HudData energyCapabilityData(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.ENERGY)
                .map(MagicLinkHudQueryPacket::energyCapabilityHudData)
                .orElse(null);
    }

    private static HudData energyCapabilityHudData(IEnergyStorage storage) {
        List<String> lines = new ArrayList<>();
        lines.add(progressLine("RF", storage.getEnergyStored(), storage.getMaxEnergyStored()));
        if (storage.canReceive()) {
            lines.add("Can receive RF");
        }
        if (storage.canExtract()) {
            lines.add("Can extract RF");
        }
        return new HudData("Energy Capability", lines);
    }

    private record HudData(String title, List<String> lines) {
    }

    private enum HudTool {
        NONE,
        MAGIC_LINKER,
        WARD_TUNER
    }
}
