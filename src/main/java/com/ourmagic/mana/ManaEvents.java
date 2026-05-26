package com.ourmagic.mana;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.spell.runtime.MagicAllies;
import com.ourmagic.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class ManaEvents {
    private ManaEvents() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 == 0) {
            PlayerMana mana = PlayerMana.get(player);
            if (mana.regenerate()) {
                ModNetwork.syncMana(player, mana);
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.syncMana(player, PlayerMana.get(player));
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.syncMana(player, PlayerMana.get(player));
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        PlayerMana.copy(event.getOriginal(), event.getEntity());
        MagicAllies.copy(event.getOriginal(), event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.syncMana(player, PlayerMana.get(player));
        }
    }
}
