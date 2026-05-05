package com.ourmagic.magic.spell.effect;

import com.ourmagic.OurMagic;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class DelayedSpellCasts {
    private static final List<ScheduledCast> CASTS = new ArrayList<>();

    private DelayedSpellCasts() {
    }

    public static void schedule(ServerLevel level, int delayTicks, Runnable cast) {
        CASTS.add(new ScheduledCast(level, level.getGameTime() + delayTicks, cast));
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CASTS.isEmpty()) {
            return;
        }

        List<ScheduledCast> due = new ArrayList<>();
        Iterator<ScheduledCast> iterator = CASTS.iterator();
        while (iterator.hasNext()) {
            ScheduledCast scheduled = iterator.next();
            if (scheduled.level.getGameTime() >= scheduled.gameTime) {
                iterator.remove();
                due.add(scheduled);
            }
        }

        for (ScheduledCast scheduled : due) {
            scheduled.cast.run();
        }
    }

    private record ScheduledCast(ServerLevel level, long gameTime, Runnable cast) {
    }
}
