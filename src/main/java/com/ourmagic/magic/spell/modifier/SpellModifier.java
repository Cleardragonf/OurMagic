package com.ourmagic.magic.spell.modifier;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellEffect;

public interface SpellModifier {
    SpellEffect wrap(Spell spell, SpellEffect effect);
}
