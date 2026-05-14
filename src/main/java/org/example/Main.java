package org.example;

import org.example.Server.ScoreBoard;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        ScoreBoard board = new ScoreBoard();
        board.registerPlayers(List.of("Alice", "Bob", "Carlos"));

        board.recordCorrectAnswer("Alice", 3000);   // answered in 3 seconds
        board.recordCorrectAnswer("Bob", 8000);     // answered in 8 seconds
        board.recordWrongAnswer("Carlos");

        board.printStandings();
        System.out.println("Winner: " + board.getWinner().getNickname());
    }
}