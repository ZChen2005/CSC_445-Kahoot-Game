package org.example.Shared;

import com.google.gson.Gson;

/**
 * Message.java
 *
 * Every packet sent between server and client is a Message.
 * It gets serialized to a single JSON string, sent over the socket,
 * and deserialized on the other side using Gson.
 *
 * Usage (server sending a question):
 *   Message msg = new Message(Message.QUESTION, gson.toJson(question));
 *   out.println(gson.toJson(msg));
 *
 * Usage (client receiving):
 *   Message msg = gson.fromJson(line, Message.class);
 *   if (msg.getType().equals(Message.QUESTION)) { ... }
 */
public class Message {

    // ─── Message type constants ────────────────────────────
    /** Client → Server: player's chosen nickname. data = nickname string. */
    public static final String NICKNAME  = "NICKNAME";

    /** Server → Client: a new question. data = JSON of a Question object. */
    public static final String QUESTION  = "QUESTION";

    /** Server → Client: countdown tick. data = seconds remaining as a string, e.g. "14". */
    public static final String TIMER     = "TIMER";

    /** Client → Server: player's answer. data = letter string: "A", "B", "C", or "D". */
    public static final String ANSWER    = "ANSWER";

    /** Server → Client: current scoreboard. data = JSON array of PlayerScore objects. */
    public static final String SCORE     = "SCORE";

    /** Server → Client: game has ended. data = same JSON scoreboard (final rankings). */
    public static final String GAME_OVER = "GAME_OVER";

    /** Server → Client: something went wrong. data = human-readable error message. */
    public static final String ERROR     = "ERROR";

    /** Server → Client: all players connected, game is about to start. data = empty. */
    public static final String START     = "START";

    // ─── Fields ────────────────────────────────────────────
    private String type;
    private String data;

    // ─── Constructors ──────────────────────────────────────
    public Message(String type, String data) {
        this.type = type;
        this.data = data;
    }

    // Required by Gson
    public Message() {}

    // ─── Getters ───────────────────────────────────────────
    public String getType() { return type; }
    public String getData() { return data; }

    // ─── Helper ────────────────────────────────────────────
    /**
     * Convenience method so callers don't need to import Gson everywhere.
     * Returns a ready-to-send JSON string of this message.
     *
     * Requires Gson on the classpath.
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * Deserializes a JSON string back into a Message object.
     *
     * @param json a raw line received from the socket
     * @return the parsed Message, or null if the line is malformed
     */
    public static Message fromJson(String json) {
        try {
            return new Gson().fromJson(json, Message.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Message{type='%s', data='%s'}", type, data);
    }
}