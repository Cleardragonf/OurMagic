package com.ourmagic.advancement;

import com.google.gson.JsonObject;
import com.ourmagic.OurMagic;
import com.ourmagic.magic.SpellRegistry;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

import java.util.Set;

public final class SpellPayloadTrigger extends SimpleCriterionTrigger<SpellPayloadTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation(OurMagic.MOD_ID, "spell_payload");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(player, GsonHelper.getAsString(json, "payload", ""));
    }

    public void trigger(ServerPlayer player, String spellKey) {
        Set<String> payloads = Set.copyOf(SpellRegistry.payloadParts(spellKey));
        if (!payloads.isEmpty()) {
            trigger(player, instance -> instance.matches(payloads));
        }
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final String payload;

        private Instance(ContextAwarePredicate player, String payload) {
            super(ID, player);
            this.payload = payload;
        }

        private boolean matches(Set<String> payloads) {
            return payload.isBlank() || payloads.contains(payload);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (!payload.isBlank()) {
                json.addProperty("payload", payload);
            }
            return json;
        }
    }
}
