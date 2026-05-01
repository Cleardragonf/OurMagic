package com.ourmagic.magic.spell.effect;

public interface PayloadEffect {
    boolean apply(SpellContext context, SpellTarget target);
}
