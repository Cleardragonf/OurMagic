package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.payloads.BlinkPayload;
import com.ourmagic.magic.spell.runtime.ComposedSpellEffect;
import com.ourmagic.magic.spell.shapes.SpellShape;
import com.ourmagic.magic.spell.shapes.SpellShapes;

public class BlinkSpell extends BaseSpell {
    public BlinkSpell() {
        this("blink", SpellShapes.lookedPointFromPlayerPosition(28, 16));
    }

    public BlinkSpell(String key, SpellShape shape) {
        super(key, 28, 70, new ComposedSpellEffect(shape, new BlinkPayload()), UPGRADE_RANGE);
    }
}
