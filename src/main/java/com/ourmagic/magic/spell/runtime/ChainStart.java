package com.ourmagic.magic.spell.runtime;

import net.minecraft.world.phys.Vec3;

public record ChainStart(Vec3 origin, int excludedEntityId) {
}
