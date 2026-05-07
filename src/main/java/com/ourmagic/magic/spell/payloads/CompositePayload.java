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
        boolean applied = false;
        for (PayloadEffect payload : payloads) {
            applied |= payload.apply(context, target);
        }
        return applied;
    }
}
