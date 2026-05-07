package com.ourmagic.magic.spell.shapes;

import com.ourmagic.magic.spell.runtime.SpellContext;

import java.util.Optional;

public interface TargetSelector {
    Optional<SpellTarget> select(SpellContext context);
}
