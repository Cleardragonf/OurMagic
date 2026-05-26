package com.ourmagic.client;

import com.ourmagic.OurMagic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OurMagicAdvancementScreenEvents {
    private static boolean openingVanilla;

    private OurMagicAdvancementScreenEvents() {
    }

    @SubscribeEvent
    public static void screenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof AdvancementsScreen)) {
            return;
        }
        if (openingVanilla) {
            openingVanilla = false;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return;
        }

        ClientAdvancements advancements = minecraft.player.connection.getAdvancements();
        if (advancements.getAdvancements().get(new ResourceLocation(OurMagic.MOD_ID, "spells/root")) == null) {
            return;
        }

        event.setNewScreen(new OurMagicAdvancementsScreen(advancements));
    }

    static void openVanilla(ClientAdvancements advancements) {
        openingVanilla = true;
        Minecraft.getInstance().setScreen(new AdvancementsScreen(advancements));
    }
}
