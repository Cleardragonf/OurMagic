package com.ourmagic.client;

import com.ourmagic.OurMagic;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID, value = Dist.CLIENT)
public final class ClientStunData {
    private static boolean active;

    private ClientStunData() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean active) {
        ClientStunData.active = active;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level == null) {
            active = false;
        }
    }
}
