package com.ourmagic.client;

import com.ourmagic.OurMagic;
import com.ourmagic.entity.WarlockEntity;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;

public class WarlockRenderer extends MobRenderer<WarlockEntity, WitchModel<WarlockEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(OurMagic.MOD_ID, "textures/entity/warlock.png");

    public WarlockRenderer(EntityRendererProvider.Context context) {
        super(context, new WitchModel<>(context.bakeLayer(ModelLayers.WITCH)), 0.5F);
        addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(WarlockEntity entity) {
        return TEXTURE;
    }
}
