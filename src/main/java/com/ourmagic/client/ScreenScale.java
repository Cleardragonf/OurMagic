package com.ourmagic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

final class ScreenScale {
    private static final float TARGET_GUI_SCALE = 2.0F;
    private static final float FIT_MARGIN = 0.96F;
    private static final float MIN_SCALE = 0.25F;
    private static final float MAX_SCALE = 2.0F;

    private ScreenScale() {
    }

    static float forDesign(Minecraft minecraft, int screenWidth, int screenHeight, int designWidth, int designHeight) {
        double guiScale = minecraft == null ? TARGET_GUI_SCALE : minecraft.getWindow().getGuiScale();
        float targetScale = TARGET_GUI_SCALE / Math.max(1.0F, (float) guiScale);
        float fitScale = Math.min(screenWidth / (float) designWidth, screenHeight / (float) designHeight) * FIT_MARGIN;
        return Mth.clamp(Math.min(targetScale, fitScale), MIN_SCALE, MAX_SCALE);
    }

    static int left(int screenWidth, float scale, int designWidth) {
        return Math.round((screenWidth / scale - designWidth) * 0.5F);
    }

    static int top(int screenHeight, float scale, int designHeight) {
        return Math.round((screenHeight / scale - designHeight) * 0.5F);
    }

    static int mouse(double coordinate, float scale) {
        return Mth.floor(coordinate / scale);
    }

    static double mouseDouble(double coordinate, float scale) {
        return coordinate / scale;
    }
}
