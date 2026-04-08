# Educational Tic-Tac-Toe AI

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/Maven-3.9+-orange)
![Swing UI](https://img.shields.io/badge/UI-Java%20Swing-6f42c1)
![Windows Installer](https://img.shields.io/badge/Installer-jpackage%20.exe-0078D6)

An educational Java Swing project that helps students compare two different ways a computer can appear "intelligent" in a game:

- a deterministic algorithm that always calculates the optimal move
- a simulation-based AI that improves through repeated trials and controlled randomness

![Game UI](assets/GameUI.png)

## Why This Project Exists

This project was built as a teaching tool.

Its purpose is not only to provide a playable Tic-Tac-Toe game, but also to help students understand:

- what an algorithm is in practice
- what classical game AI looks like without machine learning
- how different decision-making approaches produce different behavior
- why game AI is sometimes intentionally designed to be imperfect

The central teaching question is simple:

**What is the difference between an exact algorithmic opponent and an AI opponent that behaves intelligently through search and simulation?**

## Educational Focus

This project is designed to show three ideas clearly.

### 1. Deterministic algorithmic intelligence

The `Human vs Algo` mode uses **Minimax**.

Minimax explores the game tree, evaluates all relevant outcomes, and chooses the mathematically best move. For Tic-Tac-Toe, this means:

- perfect play
- no avoidable mistakes
- predictable and repeatable behavior

This is useful for teaching what a solved game looks like when a computer uses exact search.

### 2. Simulation-based AI behavior

The `Human vs AI` mode uses **Monte Carlo Tree Search (MCTS)**.

Instead of solving the entire game tree exactly, MCTS:

1. selects promising states
2. expands the search gradually
3. runs simulated games
4. uses those results to guide future decisions

This makes it a strong example of classical AI based on:

- search
- simulation
- probabilistic decision making

It does not use:

- machine learning
- neural networks
- training data

This is important for students, because it shows that "AI" is broader than modern ML.

### 3. Artificial Stupidity

This project also demonstrates a practical game-AI concept often called **Artificial Stupidity**.

The term is commonly used in game AI to describe a deliberate design choice: instead of making an opponent as strong as possible at all times, the developer intentionally limits or perturbs its behavior so that it becomes more understandable, more adjustable, or simply more enjoyable to play against.

For background, see:

- [Artificial stupidity - Wikipedia](https://en.wikipedia.org/wiki/Artificial_stupidity)

Why does this idea exist?

- because perfectly rational opponents are not always the most useful for teaching
- because "maximum strength" and "best player experience" are not the same thing
- because difficulty levels need to feel meaningfully different to the user
- because in many games, believable imperfection creates a more natural experience than robotic perfection

In small games such as Tic-Tac-Toe, even a low-budget MCTS can become very strong very quickly. If the AI always chooses the strongest move it finds, the lower settings stop feeling like lower settings.

That creates an educational problem:

- students do not clearly see the difference between approximate AI and exact search
- the slider appears to change numbers more than behavior
- the contrast with Minimax becomes weaker

To solve that, this project introduces **controlled imperfection** at lower MCTS settings.

The key idea is this:

- the AI still performs real MCTS search
- the search results still matter
- but the final action selection becomes more stochastic at lower simulation budgets

So at lower settings, the AI is more willing to:

- choose among several strong moves probabilistically
- behave less deterministically
- remain beatable and easier to compare against Minimax

At higher settings, that imperfection is reduced, so the MCTS opponent becomes stronger and more consistent.

This is not a bug and it is not "fake AI". It is a teaching-oriented application of Artificial Stupidity: the intelligence is real, but the final behavior is shaped so that students can more clearly observe the tradeoff between strength, approximation, and playability.

## Game Modes

### Human vs Algo (Minimax)

Use this mode to demonstrate:

- perfect adversarial search
- deterministic decision making
- what "optimal play" means in a solved game

Expected outcome:

- the computer should never lose
- the best result for the human player is a draw

### Human vs AI (MCTS)

Use this mode to demonstrate:

- simulation-driven search
- approximate rather than exact decision making
- how additional computation can improve behavior

The MCTS slider changes the simulation budget.

- lower values: faster, weaker, more human-like play
- higher values: stronger, more consistent play

Because this project includes controlled imperfection at low settings, this mode also helps explain why game AI is often designed for **believability and adjustability**, not only maximum strength.

## Minimax vs MCTS

| Feature | Minimax | MCTS in this project |
|---|---|---|
| Core idea | Exhaustive search | Simulation-based search |
| Behavior | Deterministic | Probabilistic |
| Strength | Perfect in Tic-Tac-Toe | Depends on simulation budget |
| Difficulty | Fixed | Adjustable |
| Mistakes | Never | Possible, especially at low settings |
| Classroom value | Explains optimal play | Explains AI tradeoffs and behavior |

## Project Structure

The implementation is intentionally organized as an educational codebase, not just a quick prototype.

- `model`: board state, players, game mode, start mode, score state
- `ai`: strategy abstraction plus `Minimax` and `MCTS`
- `controller`: game flow, mode switching, starter selection, scoreboard, AI turn handling
- `ui`: Swing window, board rendering, theme switching, win effects, background AI execution

For the full design breakdown, see [Project_Structure.md](Project_Structure.md).

## Where To Study The AI Logic

If students want to inspect how the simulation-based AI works, the most important source file is:

- [MctsStrategy.java](src/main/java/com/ai/tictactoe/ai/MctsStrategy.java)

That class contains the implementation of:

- the MCTS search loop
- rollout simulation
- backpropagation
- calibrated final move selection
- the controlled-imperfection logic described in this README

For the broader application flow, see:

- [GameController.java](src/main/java/com/ai/tictactoe/controller/GameController.java)
- [Project_Structure.md](Project_Structure.md)

## Features

- Java Swing desktop UI
- `Minimax` and `MCTS` gameplay modes
- MCTS simulation slider
- selectable starting player:
  - `Always Human`
  - `Always Pc`
  - `One by one`
- light and dark UI themes
- score tracking
- animated win effect
- background AI execution with `SwingWorker`

## Run

### Requirements

- Java 21
- Maven 3.9+

### Start the application

```bash
mvn clean compile
mvn exec:java
```

You can also run the main class directly from NetBeans:

`com.ai.tictactoe.AiTiTacToe`

## Windows Installer

This project can also be packaged as a Windows `.exe` installer using `jpackage`.

Why this matters:

- the generated installer bundles a Java runtime
- the player does not need to install Java separately
- it is suitable for sharing on GitHub releases with students or other users

### Prebuilt installer

A ready-to-use Windows installer is already included in this repository:

- [installers/Tic-Tac-Toe AI-1.0.0.exe](installers/Tic-Tac-Toe%20AI-1.0.0.exe)

This means a user can download the `.exe`, run the installer, and play without installing Java manually.

### Build locally

To build the installer locally on Windows:

```powershell
.\build-exe.ps1
```

Expected output:

- installer directory: `target/installer`
- generated Windows installer: `target/installer/Tic-Tac-Toe AI-1.0.0.exe`

Requirements for packaging:

- JDK 21 with `jpackage`
- WiX Toolset installed on Windows

If you publish the generated installer on GitHub, users should be able to download it and play without installing Java manually.

## Teaching Message

This project is meant to help students see that game intelligence can come from different ideas:

- exact reasoning
- search
- simulation
- controlled imperfection

Minimax shows what happens when the machine always plays the mathematically best move.

MCTS shows how an AI can become stronger through repeated simulations, and how developers sometimes shape that intelligence to make it more educational, more adjustable, and more interesting to play against.
