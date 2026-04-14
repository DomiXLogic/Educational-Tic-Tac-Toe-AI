package com.ai.tictactoe.model;

public enum BoardSize {
    THREE(3, 3, "3x3", 2_000, true, 9, 9, 9),
    FOUR(10, 4, "4x4", 2_200, false, 3, 12, 8),
    FIVE(10, 5, "5x5", 2_400, false, 3, 12, 8),
    SIX(10, 6, "6x6", 2_600, false, 2, 10, 7);

    private final int boardDimension;
    private final int winLength;
    private final String displayName;
    private final int recommendedSimulationCount;
    private final boolean exactAlgo;
    private final int heuristicDepth;
    private final int rootCandidateLimit;
    private final int deeperCandidateLimit;

    BoardSize(int boardDimension,
              int winLength,
              String displayName,
              int recommendedSimulationCount,
              boolean exactAlgo,
              int heuristicDepth,
              int rootCandidateLimit,
              int deeperCandidateLimit) {
        this.boardDimension = boardDimension;
        this.winLength = winLength;
        this.displayName = displayName;
        this.recommendedSimulationCount = recommendedSimulationCount;
        this.exactAlgo = exactAlgo;
        this.heuristicDepth = heuristicDepth;
        this.rootCandidateLimit = rootCandidateLimit;
        this.deeperCandidateLimit = deeperCandidateLimit;
    }

    public int getBoardDimension() {
        return boardDimension;
    }

    public int getCellCount() {
        return boardDimension * boardDimension;
    }

    public int getWinLength() {
        return winLength;
    }

    public int getRecommendedSimulationCount() {
        return recommendedSimulationCount;
    }

    public boolean usesExactAlgo() {
        return exactAlgo;
    }

    public int getHeuristicDepth() {
        return heuristicDepth;
    }

    public int getRootCandidateLimit() {
        return rootCandidateLimit;
    }

    public int getDeeperCandidateLimit() {
        return deeperCandidateLimit;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
