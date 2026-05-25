package com.ourmagic.client;

import com.ourmagic.OurMagic;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ClientWardLightData {
    private static final double WARD_GAMMA = 15.0D;
    private static boolean active;
    private static Double previousGamma;

    private ClientWardLightData() {
    }

    public static void setActive(boolean active) {
        ClientWardLightData.active = active;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null || minecraft.level == null) {
            return;
        }

        if (active) {
            if (previousGamma == null) {
                previousGamma = minecraft.options.gamma().get();
            }
            if (minecraft.options.gamma().get() < WARD_GAMMA) {
                minecraft.options.gamma().set(WARD_GAMMA);
            }
            return;
        }

        if (previousGamma != null) {
            minecraft.options.gamma().set(previousGamma);
            previousGamma = null;
        }
    }
}
