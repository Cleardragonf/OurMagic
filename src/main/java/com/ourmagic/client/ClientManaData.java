package com.ourmagic.client;

import com.ourmagic.mana.PlayerMana;

public final class ClientManaData {
    private static int mana = PlayerMana.DEFAULT_MAX_MANA;
    private static int maxMana = PlayerMana.DEFAULT_MAX_MANA;
    private static int regen = PlayerMana.DEFAULT_REGEN;
    private static int magicLevel = 1;
    private static int magicXp;
    private static int magicXpToNextLevel = 25;
    private static int magicPoints;

    private ClientManaData() {
    }

    public static void set(int mana, int maxMana, int regen, int magicLevel, int magicXp, int magicXpToNextLevel, int magicPoints) {
        ClientManaData.mana = mana;
        ClientManaData.maxMana = maxMana;
        ClientManaData.regen = regen;
        ClientManaData.magicLevel = magicLevel;
        ClientManaData.magicXp = magicXp;
        ClientManaData.magicXpToNextLevel = magicXpToNextLevel;
        ClientManaData.magicPoints = magicPoints;
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

    public static int magicLevel() {
        return magicLevel;
    }

    public static int magicXp() {
        return magicXp;
    }

    public static int magicXpToNextLevel() {
        return magicXpToNextLevel;
    }

    public static int magicPoints() {
        return magicPoints;
    }
}
