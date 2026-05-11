package com.ourmagic.client;

public final class ClientScryData {
    private static boolean active;
    private static String targetName = "";
    private static int targetIndex;
    private static int targetCount;
    private static int ticksRemaining;

    private ClientScryData() {
    }

    public static void set(boolean active, String targetName, int targetIndex, int targetCount, int ticksRemaining) {
        ClientScryData.active = active;
        ClientScryData.targetName = targetName;
        ClientScryData.targetIndex = targetIndex;
        ClientScryData.targetCount = targetCount;
        ClientScryData.ticksRemaining = ticksRemaining;
    }

    public static boolean active() {
        return active;
    }

    public static String targetName() {
        return targetName;
    }

    public static int targetIndex() {
        return targetIndex;
    }

    public static int targetCount() {
        return targetCount;
    }

    public static int ticksRemaining() {
        return ticksRemaining;
    }
}
