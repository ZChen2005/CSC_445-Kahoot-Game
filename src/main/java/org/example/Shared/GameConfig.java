package org.example.Shared;

/**
 * GameConfig.java
 *
 * Central place for all game constants.
 * Both the server and client import this file.
 * Change values here to affect the whole game.
 */
public class GameConfig {

    // Network
    /** The port the server listens on. All clients must connect to this port. */
    public static final int PORT = 5000;

    /** How long (in ms) the server waits for a client to respond before timeout. */
    public static final int SOCKET_TIMEOUT_MS = 60000; // 60 seconds

    // Players
    /** Exact number of players required to start the game. */
    public static final int MAX_PLAYERS = 2;

    /** Maximum characters allowed in a player nickname. */
    public static final int MAX_NICKNAME_LENGTH = 16;

    // Questions
    /** Path to the JSON file containing all questions (relative to server working dir). */
    public static final String QUESTIONS_FILE = "src/main/resources/questions.json";

    /** How many seconds each player has to answer a question. */
    public static final int QUESTION_TIMER_SECONDS = 15;

    /** How many seconds to show the scoreboard between questions. */
    public static final int SCOREBOARD_DISPLAY_SECONDS = 5;

    // Scoring
    /** Maximum points a player can earn for a correct answer (awarded for instant response). */
    public static final int MAX_POINTS_PER_QUESTION = 1000;

    /** Minimum points awarded for a correct answer, no matter how slow. */
    public static final int MIN_POINTS_PER_QUESTION = 100;

    /**
     * Calculates the score for a correct answer based on how fast the player responded.
     *
     * @param elapsedMs   how many milliseconds passed before the player answered
     * @param timeLimitMs the total time allowed in milliseconds
     * @return points awarded (between MIN and MAX)
     */
    public static int calculateScore(long elapsedMs, long timeLimitMs) {
        if (elapsedMs >= timeLimitMs) return MIN_POINTS_PER_QUESTION;
        double ratio = 1.0 - ((double) elapsedMs / timeLimitMs);
        int points = MIN_POINTS_PER_QUESTION + (int) (ratio * (MAX_POINTS_PER_QUESTION - MIN_POINTS_PER_QUESTION));
        return Math.min(points, MAX_POINTS_PER_QUESTION);
    }

    // Prevent instantiation
    private GameConfig() {}
}