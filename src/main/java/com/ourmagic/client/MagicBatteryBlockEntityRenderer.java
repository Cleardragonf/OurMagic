package com.ourmagic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ourmagic.OurMagic;
import com.ourmagic.block.entity.MagicBatteryBlockEntity;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MagicBatteryBlockEntityRenderer implements BlockEntityRenderer<MagicBatteryBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(OurMagic.MOD_ID, "textures/block/magic_battery.png");

    public MagicBatteryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MagicBatteryBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        MultiblockCuboidRenderer.render(blockEntity, ModBlocks.MAGIC_BATTERY.get(), TEXTURE, poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean shouldRenderOffScreen(MagicBatteryBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
