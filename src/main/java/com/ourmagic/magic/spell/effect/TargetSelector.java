package com.ourmagic.magic.spell.effect;

import java.util.Optional;

public interface TargetSelector {
    Optional<SpellTarget> select(SpellContext context);
}
