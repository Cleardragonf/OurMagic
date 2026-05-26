package com.ourmagic.client;

import com.mojang.authlib.GameProfile;
import com.ourmagic.OurMagic;
import com.ourmagic.network.IllusionDecoyPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ClientIllusions {
    private static final Map<UUID, Illusion> ILLUSIONS = new LinkedHashMap<>();

    private ClientIllusions() {
    }

    public static void add(IllusionDecoyPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Illusion illusion = new Illusion(packet, cloneProfile(packet));
        illusion.ensurePlayer(minecraft.level);
        ILLUSIONS.put(packet.id(), illusion);
    }

    private static GameProfile cloneProfile(IllusionDecoyPacket packet) {
        GameProfile copy = new GameProfile(packet.id(), packet.profile().getName());
        copy.getProperties().putAll(packet.profile().getProperties());
        return copy;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ILLUSIONS.isEmpty()) {
            ILLUSIONS.clear();
            return;
        }

        Iterator<Illusion> iterator = ILLUSIONS.values().iterator();
        while (iterator.hasNext()) {
            Illusion illusion = iterator.next();
            if (--illusion.ticks <= 0) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ILLUSIONS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            ILLUSIONS.clear();
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        for (Illusion illusion : ILLUSIONS.values()) {
            RemotePlayer player = illusion.ensurePlayer(level);
            double x = illusion.x - camera.x;
            double y = illusion.y - camera.y;
            double z = illusion.z - camera.z;
            player.tickCount = minecraft.player == null ? player.tickCount + 1 : minecraft.player.tickCount;
            minecraft.getEntityRenderDispatcher().render(player, x, y, z, illusion.yRot, event.getPartialTick(), poseStack, buffer, 15728880);
        }
        buffer.endBatch();
    }

    private static final class Illusion {
        private final GameProfile profile;
        private final double x;
        private final double y;
        private final double z;
        private final float yRot;
        private final float xRot;
        private final ItemStack head;
        private final ItemStack chest;
        private final ItemStack legs;
        private final ItemStack feet;
        private final ItemStack mainHand;
        private final ItemStack offHand;
        private RemotePlayer player;
        private int ticks;

        private Illusion(IllusionDecoyPacket packet, GameProfile profile) {
            this.profile = profile;
            this.x = packet.x();
            this.y = packet.y();
            this.z = packet.z();
            this.yRot = packet.yRot();
            this.xRot = packet.xRot();
            this.ticks = packet.ticks();
            this.head = packet.head();
            this.chest = packet.chest();
            this.legs = packet.legs();
            this.feet = packet.feet();
            this.mainHand = packet.mainHand();
            this.offHand = packet.offHand();
        }

        private RemotePlayer ensurePlayer(ClientLevel level) {
            if (player == null || player.level() != level) {
                player = new RemotePlayer(level, profile);
                player.noPhysics = true;
                applyPoseAndEquipment();
            }
            applyPoseAndEquipment();
            return player;
        }

        private void applyPoseAndEquipment() {
            player.setPos(x, y, z);
            player.setYRot(yRot);
            player.setXRot(xRot);
            player.yRotO = yRot;
            player.xRotO = xRot;
            player.yBodyRot = yRot;
            player.yBodyRotO = yRot;
            player.yHeadRot = yRot;
            player.yHeadRotO = yRot;
            player.setItemSlot(EquipmentSlot.HEAD, head.copy());
            player.setItemSlot(EquipmentSlot.CHEST, chest.copy());
            player.setItemSlot(EquipmentSlot.LEGS, legs.copy());
            player.setItemSlot(EquipmentSlot.FEET, feet.copy());
            player.setItemSlot(EquipmentSlot.MAINHAND, mainHand.copy());
            player.setItemSlot(EquipmentSlot.OFFHAND, offHand.copy());
        }
    }
}
