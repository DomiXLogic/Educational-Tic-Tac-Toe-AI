package com.ai.tictactoe.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Board {

    private static final int[][] WINNING_LINES = {
        {0, 1, 2},
        {3, 4, 5},
        {6, 7, 8},
        {0, 3, 6},
        {1, 4, 7},
        {2, 5, 8},
        {0, 4, 8},
        {2, 4, 6}
    };

    private final char[] cells;
    private final Player currentPlayer;

    public Board() {
        this(Player.X);
    }

    public Board(Player currentPlayer) {
        this(new char[9], currentPlayer);
    }

    private Board(char[] cells, Player currentPlayer) {
        if (currentPlayer == Player.EMPTY) {
            throw new IllegalArgumentException("Starting player cannot be EMPTY.");
        }
        this.cells = normalize(cells);
        this.currentPlayer = currentPlayer;
    }

    private static char[] normalize(char[] source) {
        char[] normalized = Arrays.copyOf(source, source.length);
        for (int i = 0; i < normalized.length; i++) {
            if (normalized[i] == '\0') {
                normalized[i] = Player.EMPTY.getSymbol();
            }
        }
        return normalized;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public Player getCell(int index) {
        validateIndex(index);
        return Player.fromSymbol(cells[index]);
    }

    public boolean isMoveValid(int index) {
        return index >= 0
                && index < cells.length
                && cells[index] == Player.EMPTY.getSymbol()
                && !isTerminal();
    }

    public Board makeMove(int index) {
        if (!isMoveValid(index)) {
            throw new IllegalArgumentException("Invalid move at index " + index);
        }

        char[] next = Arrays.copyOf(cells, cells.length);
        next[index] = currentPlayer.getSymbol();
        return new Board(next, currentPlayer.opponent());
    }

    public List<Integer> getAvailableMoves() {
        List<Integer> moves = new ArrayList<>();
        if (isTerminal()) {
            return moves;
        }
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == Player.EMPTY.getSymbol()) {
                moves.add(i);
            }
        }
        return moves;
    }

    public Player getWinner() {
        for (int[] line : WINNING_LINES) {
            Player player = getLineWinner(line);
            if (player != Player.EMPTY) {
                return player;
            }
        }
        return Player.EMPTY;
    }

    public int[] getWinningLine() {
        for (int[] line : WINNING_LINES) {
            Player player = getLineWinner(line);
            if (player != Player.EMPTY) {
                return Arrays.copyOf(line, line.length);
            }
        }
        return new int[0];
    }

    public boolean isDraw() {
        return getWinner() == Player.EMPTY && getAvailableMoves().isEmpty();
    }

    public boolean isTerminal() {
        return getWinner() != Player.EMPTY || isBoardFull();
    }

    public Board copy() {
        return new Board(cells, currentPlayer);
    }

    private boolean isBoardFull() {
        for (char cell : cells) {
            if (cell == Player.EMPTY.getSymbol()) {
                return false;
            }
        }
        return true;
    }

    private Player getLineWinner(int[] line) {
        char first = cells[line[0]];
        if (first == Player.EMPTY.getSymbol()) {
            return Player.EMPTY;
        }
        return first == cells[line[1]] && first == cells[line[2]]
                ? Player.fromSymbol(first)
                : Player.EMPTY;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= cells.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
    }
}
