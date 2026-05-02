package org.example.Shared;

/**
 * Question.java
 *
 * Data model for a single quiz question.
 * Loaded from questions.json by QuestionLoader on the server,
 * then sent to clients inside a Message over the network.
 */
public class Question {

    private String text;
    private String[] options; // exactly 4 options: ["A: ...", "B: ...", "C: ...", "D: ..."]
    private String answer;    // correct answer letter: "A", "B", "C", or "D"

    // Constructor
    public Question(String text, String[] options, String answer) {
        if (options == null || options.length != 4) {
            throw new IllegalArgumentException("A question must have exactly 4 options.");
        }
        if (!answer.matches("[ABCD]")) {
            throw new IllegalArgumentException("Answer must be A, B, C, or D.");
        }
        this.text    = text;
        this.options = options;
        this.answer  = answer.toUpperCase();
    }

    // Required by Gson for deserialization (no-arg constructor)
    public Question() {}

    // Getters
    public String getText()      { return text; }
    public String[] getOptions() { return options; }
    public String getAnswer()    { return answer; }

    // Helper
    /**
     * Checks whether a player's answer is correct.
     * Case-insensitive so "a" and "A" both work.
     *
     * @param playerAnswer the letter the player submitted
     * @return true if correct
     */
    public boolean isCorrect(String playerAnswer) {
        if (playerAnswer == null) return false;
        return this.answer.equalsIgnoreCase(playerAnswer.trim());
    }

    @Override
    public String toString() {
        return String.format("Question{text='%s', answer='%s'}", text, answer);
    }
}