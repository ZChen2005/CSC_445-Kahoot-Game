package org.example.Server;

import org.example.Shared.GameConfig;
import org.example.Shared.Message;
import org.example.Shared.Question;
import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.*;

/**
 * GameManager.java
 *
 * The brain of the game. Controls the entire game loop:
 *   1. Broadcasts questions to all players
 *   2. Starts a countdown timer per question
 *   3. Collects answers from PlayerHandlers via a BlockingQueue
 *   4. Updates ScoreBoard and broadcasts rankings
 *   5. Ends the game and announces the winner
 *
 * How it connects to teammates:
 *   - Server.java (teammate) creates GameManager and calls start()
 *   - Each PlayerHandler (teammate) calls submitAnswer() when a player answers
 *   - ClientGUI (teammate) receives Message objects and updates the screen
 */
public class GameManager {

    // ─── Fields ────────────────────────────────────────────
    private final List<PlayerHandler> players;   // one per connected client
    private final List<Question>      questions; // loaded from questions.json
    private final ScoreBoard          scoreBoard;
    private final Gson                gson = new Gson();

    /**
     * Each PlayerHandler drops an Answer into this queue when a player clicks.
     * GameManager reads from it during the countdown.
     * BlockingQueue is thread-safe — no synchronization needed.
     */
    private final BlockingQueue<Answer> answerQueue = new LinkedBlockingQueue<>();

    // ─── Inner class: one answer from one player ──────────
    public static class Answer {
        public final String nickname;
        public final String letter;    // "A", "B", "C", or "D"
        public final long   timestamp; // System.currentTimeMillis() when received

        public Answer(String nickname, String letter, long timestamp) {
            this.nickname  = nickname;
            this.letter    = letter;
            this.timestamp = timestamp;
        }
    }

    // ─── Constructor ───────────────────────────────────────
    public GameManager(List<PlayerHandler> players, List<Question> questions) {
        this.players    = players;
        this.questions  = questions;
        this.scoreBoard = new ScoreBoard();
    }

    // ─── Called by PlayerHandler (teammate) ───────────────
    /**
     * PlayerHandler calls this when a player sends an answer over the network.
     * It's thread-safe — multiple PlayerHandlers can call this simultaneously.
     *
     * @param nickname  the player's nickname
     * @param letter    their answer: "A", "B", "C", or "D"
     */
    public void submitAnswer(String nickname, String letter) {
        answerQueue.offer(new Answer(nickname, letter, System.currentTimeMillis()));
    }

    // ─── Main game loop ────────────────────────────────────
    /**
     * Starts the full game. Called once by Server.java after all players connect.
     * Runs synchronously — returns when the game is over.
     */
    public void start() {
        // Register all players in the scoreboard
        List<String> nicknames = players.stream()
                .map(PlayerHandler::getNickname)
                .toList();
        scoreBoard.registerPlayers(nicknames);

        // Tell all clients the game is starting
        broadcast(new Message(Message.START, "Game is starting!"));
        sleep(2000);

        // ── Question loop ──
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            System.out.printf("%n[GameManager] Question %d/%d: %s%n",
                    i + 1, questions.size(), question.getText());

            playQuestion(question, i + 1);

            // Show scoreboard between questions
            broadcastScoreboard();
            scoreBoard.printStandings();

            // Pause before next question (skip pause after last question)
            if (i < questions.size() - 1) {
                sleep(GameConfig.SCOREBOARD_DISPLAY_SECONDS * 1000L);
            }
        }

        // ── Game over ──
        broadcastGameOver();
        System.out.println("[GameManager] Game over! Winner: " + scoreBoard.getWinner().getNickname());
    }

    // ─── Single question round ─────────────────────────────
    private void playQuestion(Question question, int questionNumber) {
        // Clear any leftover answers from previous round
        answerQueue.clear();

        // Track who has already answered this round (no double answers)
        java.util.Set<String> answered = new java.util.HashSet<>();

        // Send the question to all clients
        String questionJson = gson.toJson(question);
        broadcast(new Message(Message.QUESTION, questionJson));

        long startTime   = System.currentTimeMillis();
        long timeLimitMs = GameConfig.QUESTION_TIMER_SECONDS * 1000L;
        long endTime     = startTime + timeLimitMs;

        // ── Countdown loop ──
        // Runs until time is up OR all players have answered
        while (System.currentTimeMillis() < endTime && answered.size() < players.size()) {

            // Broadcast the remaining seconds once per second
            long remaining = (endTime - System.currentTimeMillis()) / 1000;
            broadcast(new Message(Message.TIMER, String.valueOf(remaining)));

            // Check the queue for new answers (wait up to 1 second)
            try {
                Answer answer = answerQueue.poll(1, TimeUnit.SECONDS);
                if (answer != null && !answered.contains(answer.nickname)) {
                    answered.add(answer.nickname);
                    processAnswer(answer, question, startTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Any player who didn't answer in time gets recorded as wrong
        for (PlayerHandler player : players) {
            if (!answered.contains(player.getNickname())) {
                scoreBoard.recordWrongAnswer(player.getNickname());
                System.out.printf("[GameManager] %s did not answer in time%n",
                        player.getNickname());
            }
        }

        // Send TIMER = 0 so clients know the round closed
        broadcast(new Message(Message.TIMER, "0"));
    }

    // ─── Process one answer ────────────────────────────────
    private void processAnswer(Answer answer, Question question, long startTime) {
        long elapsedMs = answer.timestamp - startTime;

        if (question.isCorrect(answer.letter)) {
            scoreBoard.recordCorrectAnswer(answer.nickname, elapsedMs);
            System.out.printf("[GameManager] %s answered CORRECT in %dms%n",
                    answer.nickname, elapsedMs);
        } else {
            scoreBoard.recordWrongAnswer(answer.nickname);
            System.out.printf("[GameManager] %s answered WRONG (%s, correct: %s)%n",
                    answer.nickname, answer.letter, question.getAnswer());
        }
    }

    // ─── Broadcast helpers ─────────────────────────────────
    /**
     * Sends a message to every connected player.
     */
    private void broadcast(Message message) {
        String json = gson.toJson(message);
        for (PlayerHandler player : players) {
            player.sendMessage(json);
        }
    }

    /**
     * Serializes the current scoreboard and broadcasts it to all clients.
     */
    private void broadcastScoreboard() {
        String rankingsJson = gson.toJson(scoreBoard.getRankings());
        broadcast(new Message(Message.SCORE, rankingsJson));
    }

    /**
     * Broadcasts GAME_OVER with the final scoreboard.
     */
    private void broadcastGameOver() {
        String rankingsJson = gson.toJson(scoreBoard.getRankings());
        broadcast(new Message(Message.GAME_OVER, rankingsJson));
    }

    // ─── Utility ───────────────────────────────────────────
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
