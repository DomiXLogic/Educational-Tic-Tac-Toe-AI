package com.ai.tictactoe.model;

public enum ArtificialStupidityLevel {
    SUPER_LOW("Super Low", 0, 0.12),
    LOW("Low", 1, 0.35),
    MEDIUM("Medium", 2, 0.58),
    HIGH("High", 3, 0.80),
    EXTRA_HIGH("Extra High", 4, 1.00);

    private final String displayName;
    private final int sliderValue;
    private final double imperfectionMultiplier;

    ArtificialStupidityLevel(String displayName, int sliderValue, double imperfectionMultiplier) {
        this.displayName = displayName;
        this.sliderValue = sliderValue;
        this.imperfectionMultiplier = imperfectionMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSliderValue() {
        return sliderValue;
    }

    public double getImperfectionMultiplier() {
        return imperfectionMultiplier;
    }

    public static ArtificialStupidityLevel fromSliderValue(int sliderValue) {
        for (ArtificialStupidityLevel level : values()) {
            if (level.sliderValue == sliderValue) {
                return level;
            }
        }
        return EXTRA_HIGH;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
