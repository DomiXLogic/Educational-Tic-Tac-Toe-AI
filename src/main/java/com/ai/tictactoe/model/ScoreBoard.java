package com.ai.tictactoe.model;

public record ScoreBoard(int humanWins, int computerWins, int draws) {

    public static ScoreBoard empty() {
        return new ScoreBoard(0, 0, 0);
    }

    public ScoreBoard withHumanWin() {
        return new ScoreBoard(humanWins + 1, computerWins, draws);
    }

    public ScoreBoard withComputerWin() {
        return new ScoreBoard(humanWins, computerWins + 1, draws);
    }

    public ScoreBoard withDraw() {
        return new ScoreBoard(humanWins, computerWins, draws + 1);
    }
}
