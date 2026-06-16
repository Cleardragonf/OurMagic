package com.ourmagic.magic.energy;

public enum MagicTransferMode {
    OUTPUT("Output"),
    INPUT("Input"),
    BOTH("Input/Output"),
    DISABLED("Disabled");

    private final String displayName;

    MagicTransferMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean canInput() {
        return this == INPUT || this == BOTH;
    }

    public boolean canOutput() {
        return this == OUTPUT || this == BOTH;
    }

    public MagicTransferMode next() {
        MagicTransferMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }
}
