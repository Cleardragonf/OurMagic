package com.ourmagic.client;

import net.minecraft.core.BlockPos;

import java.util.List;

public final class MagicLinkHudData {
    private static BlockPos pos;
    private static String title = "";
    private static List<String> lines = List.of();
    private static long expiresAt;

    private MagicLinkHudData() {
    }

    public static void set(BlockPos targetPos, String targetTitle, List<String> targetLines, long gameTime) {
        pos = targetPos.immutable();
        title = targetTitle;
        lines = List.copyOf(targetLines);
        expiresAt = gameTime + 20;
    }

    public static boolean active(BlockPos targetPos, long gameTime) {
        return pos != null && pos.equals(targetPos) && gameTime <= expiresAt && !title.isBlank();
    }

    public static String title() {
        return title;
    }

    public static List<String> lines() {
        return lines;
    }
}
