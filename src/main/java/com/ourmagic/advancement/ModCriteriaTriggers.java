package com.ourmagic.advancement;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;

public final class ModCriteriaTriggers {
    public static final SpellPayloadTrigger SPELL_PAYLOAD = CriteriaTriggers.register(new SpellPayloadTrigger());

    private ModCriteriaTriggers() {
    }

    public static void register() {
    }

    public static void awardSpell(ServerPlayer player, String spellKey) {
        SPELL_PAYLOAD.trigger(player, spellKey);
    }
}
