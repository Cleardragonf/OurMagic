package com.ourmagic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ourmagic.block.entity.TeleportPortalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TeleportPortalBlockEntityRenderer implements BlockEntityRenderer<TeleportPortalBlockEntity> {
    private static final float BEAM_DURATION = 18.0F;

    public TeleportPortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TeleportPortalBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float age = blockEntity.getLevel() == null ? 0.0F : blockEntity.getLevel().getGameTime() + partialTick;
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());

        if (blockEntity.targetPortal().isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.035D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 2.2F));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        ring(consumer, matrix, normal, packedLight, 2.0F, 72, 0.22F, 0.95F, 1.0F, 0.72F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-age * 4.7F));
        ring(consumer, poseStack.last().pose(), poseStack.last().normal(), packedLight, 1.32F, 36, 0.75F, 0.33F, 1.0F, 0.58F);
        spokes(consumer, poseStack.last().pose(), poseStack.last().normal(), packedLight, age);

        poseStack.popPose();

        renderTeleportBeam(blockEntity, age, poseStack, consumer, packedLight);
    }

    private static void renderTeleportBeam(TeleportPortalBlockEntity blockEntity, float age, PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        float beamAge = age - blockEntity.beamTick();
        if (blockEntity.beamDirection() == 0 || beamAge < 0.0F || beamAge > BEAM_DURATION) {
            return;
        }

        float progress = beamAge / BEAM_DURATION;
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        float y = blockEntity.beamDirection() > 0 ? eased * 3.1F : (1.0F - eased) * 3.1F;
        float alpha = 0.72F * (1.0F - progress);
        float radius = 1.85F - progress * 0.35F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.08D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 7.0F));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        ringAtY(consumer, matrix, normal, packedLight, radius, y, 64, 0.45F, 0.98F, 1.0F, alpha);
        beamColumn(consumer, matrix, normal, packedLight, radius * 0.52F, y, alpha * 0.55F);

        poseStack.popPose();
    }

    private static void ring(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, float radius, int segments, float red, float green, float blue, float alpha) {
        double step = Math.PI * 2.0D / segments;
        for (int i = 0; i < segments; i++) {
            if (i % 4 == 3) {
                continue;
            }
            double a1 = i * step;
            double a2 = (i + 0.72D) * step;
            Vec3 p1 = new Vec3(Math.cos(a1) * radius, 0.0D, Math.sin(a1) * radius);
            Vec3 p2 = new Vec3(Math.cos(a2) * radius, 0.0D, Math.sin(a2) * radius);
            line(consumer, matrix, normal, packedLight, p1, p2, red, green, blue, alpha);
        }
    }

    private static void spokes(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, float age) {
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3.0D + age * 0.035D;
            Vec3 inner = new Vec3(Math.cos(angle) * 0.55D, 0.0D, Math.sin(angle) * 0.55D);
            Vec3 outer = new Vec3(Math.cos(angle) * 1.72D, 0.0D, Math.sin(angle) * 1.72D);
            line(consumer, matrix, normal, packedLight, inner, outer, 0.95F, 0.55F, 1.0F, 0.45F);
        }
    }

    private static void ringAtY(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, float radius, float y, int segments, float red, float green, float blue, float alpha) {
        double step = Math.PI * 2.0D / segments;
        for (int i = 0; i < segments; i++) {
            if (i % 5 == 4) {
                continue;
            }
            double a1 = i * step;
            double a2 = (i + 0.66D) * step;
            Vec3 p1 = new Vec3(Math.cos(a1) * radius, y, Math.sin(a1) * radius);
            Vec3 p2 = new Vec3(Math.cos(a2) * radius, y, Math.sin(a2) * radius);
            line3d(consumer, matrix, normal, packedLight, p1, p2, red, green, blue, alpha, 0.04F);
        }
    }

    private static void beamColumn(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, float radius, float y, float alpha) {
        float low = Math.max(0.0F, y - 0.95F);
        float high = Math.min(3.2F, y + 0.95F);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0D;
            Vec3 start = new Vec3(Math.cos(angle) * radius, low, Math.sin(angle) * radius);
            Vec3 end = new Vec3(Math.cos(angle) * radius, high, Math.sin(angle) * radius);
            line3d(consumer, matrix, normal, packedLight, start, end, 0.35F, 0.9F, 1.0F, alpha, 0.025F);
        }
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, Vec3 start, Vec3 end, float red, float green, float blue, float alpha) {
        line3d(consumer, matrix, normal, packedLight, start, end, red, green, blue, alpha, 0.028F);
    }

    private static void line3d(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, Vec3 start, Vec3 end, float red, float green, float blue, float alpha, float width) {
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        float ox = length < 0.0001D ? width : (float) (-dz / length) * width;
        float oz = length < 0.0001D ? 0.0F : (float) (dx / length) * width;
        quad3d(consumer, matrix, normal, packedLight,
                (float) start.x - ox, (float) start.y, (float) start.z - oz,
                (float) start.x + ox, (float) start.y, (float) start.z + oz,
                (float) end.x + ox, (float) end.y, (float) end.z + oz,
                (float) end.x - ox, (float) end.y, (float) end.z - oz,
                red, green, blue, alpha);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight,
                             float x1, float z1, float x2, float z2, float x3, float z3, float x4, float z4,
                             float red, float green, float blue, float alpha) {
        quad3d(consumer, matrix, normal, packedLight,
                x1, 0.0F, z1,
                x2, 0.0F, z2,
                x3, 0.0F, z3,
                x4, 0.0F, z4,
                red, green, blue, alpha);
    }

    private static void quad3d(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float x3, float y3, float z3, float x4, float y4, float z4,
                               float red, float green, float blue, float alpha) {
        vertex(consumer, matrix, normal, packedLight, x1, y1, z1, red, green, blue, alpha);
        vertex(consumer, matrix, normal, packedLight, x2, y2, z2, red, green, blue, alpha);
        vertex(consumer, matrix, normal, packedLight, x3, y3, z3, red, green, blue, alpha);
        vertex(consumer, matrix, normal, packedLight, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, float x, float y, float z, float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(0)
                .uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(TeleportPortalBlockEntity blockEntity) {
        return true;
    }
}
