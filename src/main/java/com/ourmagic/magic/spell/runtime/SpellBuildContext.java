package com.ourmagic.magic.spell.runtime;

import java.util.List;

public record SpellBuildContext(String spellKey, String payloadKey, String shapeKey, List<String> payloadKeys, int payloadIndex) {
    public SpellBuildContext {
        payloadKeys = List.copyOf(payloadKeys);
    }

    public boolean areaShape() {
        return shapeKey.contains("aoe") || shapeKey.contains("area");
    }

    public boolean selfShape() {
        return shapeKey.contains("self");
    }
}
