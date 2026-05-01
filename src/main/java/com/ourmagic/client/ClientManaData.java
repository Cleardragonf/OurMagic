package com.ourmagic.client;

import com.ourmagic.mana.PlayerMana;

public final class ClientManaData {
    private static int mana = PlayerMana.DEFAULT_MAX_MANA;
    private static int maxMana = PlayerMana.DEFAULT_MAX_MANA;
    private static int regen = PlayerMana.DEFAULT_REGEN;

    private ClientManaData() {
    }

    public static void set(int mana, int maxMana, int regen) {
        ClientManaData.mana = mana;
        ClientManaData.maxMana = maxMana;
        ClientManaData.regen = regen;
    }

    public static int mana() {
        return mana;
    }

    public static int maxMana() {
        return maxMana;
    }

    public static int regen() {
        return regen;
    }
}
