package com.ourmagic.client;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModBlockEntities;
import com.ourmagic.registry.ModBlocks;
import com.ourmagic.registry.ModEntities;
import com.ourmagic.registry.ModMenus;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.WAND.get(), WandScreen::new);
            MenuScreens.register(ModMenus.SPELLCRAFT.get(), SpellcraftScreen::new);
            MenuScreens.register(ModMenus.PLAYER_UPGRADES.get(), PlayerUpgradeScreen::new);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TEMPORARY_SHIELD.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WARD_BOUNDARY.get(), RenderType.translucent());
            BlockEntityRenderers.register(ModBlockEntities.WARD_CAMOUFLAGE.get(), WardCamouflageBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MAGIC_BATTERY.get(), MagicBatteryBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.WARD_STONE.get(), WardStoneBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.MAGIC_RELAY.get(), MagicRelayBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CREATIVE_RF_GENERATOR.get(), CreativeRfGeneratorBlockEntityRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.TELEPORT_PORTAL.get(), TeleportPortalBlockEntityRenderer::new);
        });
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                ? BiomeColors.getAverageWaterColor(level, pos)
                : 0x3F76E4, ModBlocks.WARD_BOUNDARY.get());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WARLOCK.get(), WarlockRenderer::new);
    }
}
