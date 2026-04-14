package com.ai.tictactoe.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Board {

    private final char[] cells;
    private final Player currentPlayer;
    private final BoardSize boardSize;

    public Board() {
        this(Player.X, BoardSize.THREE);
    }

    public Board(Player currentPlayer) {
        this(currentPlayer, BoardSize.THREE);
    }

    public Board(Player currentPlayer, BoardSize boardSize) {
        this(new char[boardSize.getCellCount()], currentPlayer, boardSize);
    }

    private Board(char[] cells, Player currentPlayer, BoardSize boardSize) {
        if (currentPlayer == Player.EMPTY) {
            throw new IllegalArgumentException("Starting player cannot be EMPTY.");
        }
        if (cells.length != boardSize.getCellCount()) {
            throw new IllegalArgumentException("Cell count does not match board size " + boardSize + ".");
        }
        this.cells = normalize(cells);
        this.currentPlayer = currentPlayer;
        this.boardSize = boardSize;
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

    public BoardSize getBoardSize() {
        return boardSize;
    }

    public int getSize() {
        return boardSize.getBoardDimension();
    }

    public int getCellCount() {
        return cells.length;
    }

    public int getWinLength() {
        return boardSize.getWinLength();
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
        return new Board(next, currentPlayer.opponent(), boardSize);
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
        int[] line = findWinningLine();
        if (line.length == 0) {
            return Player.EMPTY;
        }
        return getCell(line[0]);
    }

    public int[] getWinningLine() {
        return findWinningLine();
    }

    public boolean isDraw() {
        return getWinner() == Player.EMPTY && getAvailableMoves().isEmpty();
    }

    public boolean isTerminal() {
        return getWinner() != Player.EMPTY || isBoardFull();
    }

    public Board copy() {
        return new Board(cells, currentPlayer, boardSize);
    }

    public List<int[]> getPotentialWinningLines() {
        List<int[]> lines = new ArrayList<>();
        int boardDimension = getSize();
        int winLength = getWinLength();

        for (int row = 0; row < boardDimension; row++) {
            for (int column = 0; column <= boardDimension - winLength; column++) {
                int[] line = new int[winLength];
                for (int offset = 0; offset < winLength; offset++) {
                    line[offset] = row * boardDimension + (column + offset);
                }
                lines.add(line);
            }
        }

        for (int column = 0; column < boardDimension; column++) {
            for (int row = 0; row <= boardDimension - winLength; row++) {
                int[] line = new int[winLength];
                for (int offset = 0; offset < winLength; offset++) {
                    line[offset] = (row + offset) * boardDimension + column;
                }
                lines.add(line);
            }
        }

        for (int row = 0; row <= boardDimension - winLength; row++) {
            for (int column = 0; column <= boardDimension - winLength; column++) {
                int[] line = new int[winLength];
                for (int offset = 0; offset < winLength; offset++) {
                    line[offset] = (row + offset) * boardDimension + (column + offset);
                }
                lines.add(line);
            }
        }

        for (int row = 0; row <= boardDimension - winLength; row++) {
            for (int column = winLength - 1; column < boardDimension; column++) {
                int[] line = new int[winLength];
                for (int offset = 0; offset < winLength; offset++) {
                    line[offset] = (row + offset) * boardDimension + (column - offset);
                }
                lines.add(line);
            }
        }

        return lines;
    }

    private boolean isBoardFull() {
        for (char cell : cells) {
            if (cell == Player.EMPTY.getSymbol()) {
                return false;
            }
        }
        return true;
    }

    private int[] findWinningLine() {
        for (int[] line : getPotentialWinningLines()) {
            if (isWinningLine(line)) {
                return line;
            }
        }

        return new int[0];
    }

    private boolean isWinningLine(int[] line) {
        char first = cells[line[0]];
        if (first == Player.EMPTY.getSymbol()) {
            return false;
        }

        for (int i = 1; i < line.length; i++) {
            if (cells[line[i]] != first) {
                return false;
            }
        }
        return true;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= cells.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
    }
}
