package com.ai.tictactoe.model;

public enum Player {
    X('X'),
    O('O'),
    EMPTY(' ');

    private final char symbol;

    Player(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public Player opponent() {
        return switch (this) {
            case X -> O;
            case O -> X;
            case EMPTY -> EMPTY;
        };
    }

    public static Player fromSymbol(char symbol) {
        for (Player player : values()) {
            if (player.symbol == symbol) {
                return player;
            }
        }
        throw new IllegalArgumentException("Unknown player symbol: " + symbol);
    }
}
