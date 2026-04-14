package com.ai.tictactoe.model;

public enum BotStrategyType {
    ALGO("Algo"),
    MCTS("MCTS");

    private final String displayName;

    BotStrategyType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
