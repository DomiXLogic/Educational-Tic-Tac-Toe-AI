package com.ai.tictactoe.model;

public enum GameMode {
    HUMAN_VS_AI_MCTS("Human vs AI (MCTS)", true),
    HUMAN_VS_ALGO_MINIMAX("Human vs Algo", false),
    PC_VS_PC("PC vs PC", false);

    private final String displayName;
    private final boolean usesSimulationSlider;

    GameMode(String displayName, boolean usesSimulationSlider) {
        this.displayName = displayName;
        this.usesSimulationSlider = usesSimulationSlider;
    }

    public boolean usesSimulationSlider() {
        return usesSimulationSlider;
    }

    public boolean isPcVsPc() {
        return this == PC_VS_PC;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
