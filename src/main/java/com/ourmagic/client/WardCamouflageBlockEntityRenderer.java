package com.ourmagic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ourmagic.block.entity.WardCamouflageBlockEntity;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class WardCamouflageBlockEntityRenderer implements BlockEntityRenderer<WardCamouflageBlockEntity> {
    public WardCamouflageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WardCamouflageBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState mimic = blockEntity.mimicState();
        if (mimic.isAir()
                || mimic.is(Blocks.LIGHT)
                || mimic.is(ModBlocks.WARD_CAMOUFLAGE.get())
                || mimic.is(ModBlocks.WARD_BOUNDARY.get())
                || mimic.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(mimic, poseStack, buffer, packedLight, packedOverlay);
    }
}
