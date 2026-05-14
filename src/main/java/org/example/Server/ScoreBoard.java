package org.example.Server;

import org.example.Shared.GameConfig;
import java.util.*;

/**
 * ScoreBoard.java
 *
 * Tracks every player's score during the game.
 * Called by GameManager after each question to update points
 * and retrieve the current rankings to broadcast to all clients.
 */
public class ScoreBoard {

    // ─── Inner class: one entry per player ────────────────
    public static class PlayerScore {
        private String nickname;
        private int score;
        private int correctAnswers;

        public PlayerScore(String nickname) {
            this.nickname       = nickname;
            this.score          = 0;
            this.correctAnswers = 0;
        }

        public String getNickname()      { return nickname; }
        public int    getScore()         { return score; }
        public int    getCorrectAnswers(){ return correctAnswers; }

        // Called by ScoreBoard when a correct answer comes in
        void addPoints(int points) {
            this.score += points;
            this.correctAnswers++;
        }

        @Override
        public String toString() {
            return String.format("%s: %d pts (%d correct)", nickname, score, correctAnswers);
        }
    }

    // ─── Fields ────────────────────────────────────────────
    // Map of nickname -> PlayerScore for fast lookup
    private final Map<String, PlayerScore> scores = new LinkedHashMap<>();

    // ─── Setup ─────────────────────────────────────────────
    /**
     * Register all players before the game starts.
     * Called by GameManager once all clients have connected.
     *
     * @param nicknames list of all player nicknames
     */
    public void registerPlayers(List<String> nicknames) {
        scores.clear();
        for (String nickname : nicknames) {
            scores.put(nickname, new PlayerScore(nickname));
        }
    }

    // ─── Core logic ────────────────────────────────────────
    /**
     * Record a correct answer and award points based on speed.
     *
     * @param nickname    the player who answered correctly
     * @param elapsedMs   how many ms passed before they answered
     */
    public void recordCorrectAnswer(String nickname, long elapsedMs) {
        PlayerScore player = scores.get(nickname);
        if (player == null) return;

        long timeLimitMs = GameConfig.QUESTION_TIMER_SECONDS * 1000L;
        int points = GameConfig.calculateScore(elapsedMs, timeLimitMs);
        player.addPoints(points);

        System.out.printf("[ScoreBoard] %s answered correctly in %dms → +%d pts%n",
                nickname, elapsedMs, points);
    }

    /**
     * Record a wrong or missing answer. No points awarded.
     *
     * @param nickname the player who answered wrong or timed out
     */
    public void recordWrongAnswer(String nickname) {
        System.out.printf("[ScoreBoard] %s answered wrong or timed out → +0 pts%n", nickname);
    }

    // ─── Rankings ──────────────────────────────────────────
    /**
     * Returns all players sorted by score descending.
     * Used by GameManager to broadcast after each question.
     *
     * @return sorted list of PlayerScore (highest score first)
     */
    public List<PlayerScore> getRankings() {
        List<PlayerScore> list = new ArrayList<>(scores.values());
        list.sort((a, b) -> b.getScore() - a.getScore());
        return list;
    }

    /**
     * Returns the winner (player with the highest score).
     * Called at the end of the game.
     */
    public PlayerScore getWinner() {
        return getRankings().get(0);
    }

    /**
     * Prints the current standings to the server console.
     * Useful for debugging.
     */
    public void printStandings() {
        System.out.println("─── Current standings ───────────────");
        List<PlayerScore> rankings = getRankings();
        for (int i = 0; i < rankings.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, rankings.get(i));
        }
        System.out.println("─────────────────────────────────────");
    }
}