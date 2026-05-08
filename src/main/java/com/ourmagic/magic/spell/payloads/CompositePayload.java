package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import java.util.List;

public class CompositePayload implements PayloadEffect {
    private final List<PayloadEffect> payloads;

    public CompositePayload(PayloadEffect... payloads) {
        this.payloads = List.of(payloads);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        List<PayloadEffect> runePayloads = payloads.stream().filter(payload -> payload instanceof RunePayload).toList();
        if (!runePayloads.isEmpty()) {
            List<PayloadEffect> triggeredPayloads = payloads.stream().filter(payload -> !(payload instanceof RunePayload)).toList();
            return ((RunePayload) runePayloads.get(0)).place(context, target, triggeredPayloads);
        }

        boolean echo = payloads.stream().anyMatch(payload -> payload instanceof EchoPayload);
        List<PayloadEffect> immediatePayloads = payloads.stream().filter(payload -> !(payload instanceof EchoPayload)).toList();
        boolean applied = false;
        for (PayloadEffect payload : immediatePayloads) {
            applied |= payload.apply(context, target);
        }
        if (echo && !immediatePayloads.isEmpty()) {
            EchoPayload.scheduleEcho(context, target, immediatePayloads);
            applied = true;
        }
        return applied;
    }
}
