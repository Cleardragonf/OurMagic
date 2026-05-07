package com.ourmagic.magic.spell.shapes;

import com.ourmagic.magic.spell.runtime.SpellContext;

import java.util.List;

public interface AreaSelector {
    List<SpellTarget> select(SpellContext context, SpellTarget origin);

    double radius(SpellContext context);
}
