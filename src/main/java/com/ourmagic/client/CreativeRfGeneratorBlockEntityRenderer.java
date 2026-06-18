package com.ourmagic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ourmagic.OurMagic;
import com.ourmagic.block.entity.CreativeRfGeneratorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CreativeRfGeneratorBlockEntityRenderer implements BlockEntityRenderer<CreativeRfGeneratorBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(OurMagic.MOD_ID, "textures/block/creative_rf_generator.png");

    public CreativeRfGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CreativeRfGeneratorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float age = blockEntity.getLevel() == null ? 0.0F : blockEntity.getLevel().getGameTime() + partialTick;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        box(consumer, matrix, normal, packedLight, packedOverlay, 0.06F, 0.00F, 0.13F, 0.94F, 0.50F, 0.87F);
        box(consumer, matrix, normal, packedLight, packedOverlay, 0.19F, 0.50F, 0.25F, 0.81F, 0.75F, 0.75F);
        box(consumer, matrix, normal, packedLight, packedOverlay, 0.31F, 0.75F, 0.31F, 0.69F, 0.94F, 0.69F);
        box(consumer, matrix, normal, packedLight, packedOverlay, 0.38F, 0.94F, 0.38F, 0.62F, 1.00F, 0.62F);

        renderCog(poseStack, consumer, packedLight, packedOverlay, age, 0.25F, 0.51F, -1.0F);
        renderCog(poseStack, consumer, packedLight, packedOverlay, age, 0.75F, 0.51F, 1.0F);
    }

    private static void renderCog(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float age, float centerX, float centerY, float direction) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.105D);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(age * 12.0F * direction));
        poseStack.translate(-centerX, -centerY, -0.105D);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        box(consumer, matrix, normal, packedLight, packedOverlay, centerX - 0.10F, centerY - 0.10F, 0.06F, centerX + 0.10F, centerY + 0.10F, 0.15F);
        box(consumer, matrix, normal, packedLight, packedOverlay, centerX - 0.025F, centerY - 0.18F, 0.055F, centerX + 0.025F, centerY + 0.18F, 0.155F);
        box(consumer, matrix, normal, packedLight, packedOverlay, centerX - 0.18F, centerY - 0.025F, 0.055F, centerX + 0.18F, centerY + 0.025F, 0.155F);
        box(consumer, matrix, normal, packedLight, packedOverlay, centerX - 0.14F, centerY - 0.14F, 0.055F, centerX - 0.09F, centerY - 0.09F, 0.155F);
        box(consumer, matrix, normal, packedLight, packedOverlay, centerX + 0.09F, centerY - 0.14F, 0.055F, centerX + 0.14F, centerY - 0.09F, 0.155F);
        box(consumer, matrix, normal, packedLight, packedOverlay, centerX - 0.14F, centerY + 0.09F, 0.055F, centerX - 0.09F, centerY + 0.14F, 0.155F);
        box(consumer, matrix, normal, packedLight, packedOverlay, centerX + 0.09F, centerY + 0.09F, 0.055F, centerX + 0.14F, centerY + 0.14F, 0.155F);
        poseStack.popPose();
    }

    private static void box(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, int packedOverlay,
                            float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        quad(consumer, matrix, normal, packedLight, packedOverlay, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0, 0, 1);
        quad(consumer, matrix, normal, packedLight, packedOverlay, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0, 0, -1);
        quad(consumer, matrix, normal, packedLight, packedOverlay, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1, 0, 0);
        quad(consumer, matrix, normal, packedLight, packedOverlay, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1, 0, 0);
        quad(consumer, matrix, normal, packedLight, packedOverlay, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0, 1, 0);
        quad(consumer, matrix, normal, packedLight, packedOverlay, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0, -1, 0);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, int packedOverlay,
                             float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
                             float normalX, float normalY, float normalZ) {
        vertex(consumer, matrix, normal, packedLight, packedOverlay, x1, y1, z1, 0.0F, 1.0F, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay, x2, y2, z2, 1.0F, 1.0F, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay, x3, y3, z3, 1.0F, 0.0F, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay, x4, y4, z4, 0.0F, 0.0F, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, int packedOverlay,
                               float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}
