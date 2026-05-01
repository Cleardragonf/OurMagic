package com.ourmagic.wand;

import java.util.List;

public record WandTemplate(
        String key,
        String displayName,
        float power,
        List<WandData.WandSpellData> spells
) {
    public String activeSpell() {
        return spells.isEmpty() ? "" : spells.get(0).key();
    }
}
