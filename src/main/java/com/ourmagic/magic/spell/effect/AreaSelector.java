package com.ourmagic.magic.spell.effect;

import java.util.List;

public interface AreaSelector {
    List<SpellTarget> select(SpellContext context, SpellTarget origin);

    double radius(SpellContext context);
}
