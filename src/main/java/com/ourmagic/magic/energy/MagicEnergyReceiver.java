package com.ourmagic.magic.energy;

public interface MagicEnergyReceiver {
    int receiveMagicEnergy(MagicEnergyType type, int amount, boolean simulate);
}
