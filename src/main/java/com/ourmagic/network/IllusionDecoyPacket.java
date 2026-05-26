package com.ourmagic.network;

import com.mojang.authlib.GameProfile;
import com.ourmagic.client.ClientIllusions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record IllusionDecoyPacket(UUID id, GameProfile profile, double x, double y, double z, float yRot, float xRot,
                                  int ticks, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet,
                                  ItemStack mainHand, ItemStack offHand) {
    public static void encode(IllusionDecoyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.id);
        buffer.writeNbt(NbtUtils.writeGameProfile(new CompoundTag(), packet.profile));
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.yRot);
        buffer.writeFloat(packet.xRot);
        buffer.writeVarInt(packet.ticks);
        buffer.writeItem(packet.head);
        buffer.writeItem(packet.chest);
        buffer.writeItem(packet.legs);
        buffer.writeItem(packet.feet);
        buffer.writeItem(packet.mainHand);
        buffer.writeItem(packet.offHand);
    }

    public static IllusionDecoyPacket decode(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        CompoundTag profileTag = buffer.readNbt();
        GameProfile profile = profileTag == null ? new GameProfile(id, "Illusion") : NbtUtils.readGameProfile(profileTag);
        return new IllusionDecoyPacket(id, profile, buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(), buffer.readItem(), buffer.readItem(),
                buffer.readItem(), buffer.readItem(), buffer.readItem(), buffer.readItem());
    }

    public static IllusionDecoyPacket from(UUID id, GameProfile profile, double x, double y, double z, float yRot,
                                           float xRot, int ticks, java.util.function.Function<EquipmentSlot, ItemStack> equipment) {
        return new IllusionDecoyPacket(id, profile, x, y, z, yRot, xRot, ticks,
                equipment.apply(EquipmentSlot.HEAD).copy(),
                equipment.apply(EquipmentSlot.CHEST).copy(),
                equipment.apply(EquipmentSlot.LEGS).copy(),
                equipment.apply(EquipmentSlot.FEET).copy(),
                equipment.apply(EquipmentSlot.MAINHAND).copy(),
                equipment.apply(EquipmentSlot.OFFHAND).copy());
    }

    public static void handle(IllusionDecoyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientIllusions.add(packet)));
        context.setPacketHandled(true);
    }
}
