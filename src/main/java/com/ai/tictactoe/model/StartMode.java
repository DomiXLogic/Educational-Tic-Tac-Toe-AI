package com.ai.tictactoe.model;

public enum StartMode {
    ALWAYS_HUMAN("Always Human"),
    ALWAYS_PC("Always Pc"),
    ONE_BY_ONE("One by one");

    private final String displayName;

    StartMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
