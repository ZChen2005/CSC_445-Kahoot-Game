package org.example.Server;

/**
 * PlayerHandler.java - STUB
 *
 * Your teammate will implement this fully.
 * This stub only exists so GameManager compiles on your machine.
 *
 * The real PlayerHandler will:
 *   - Hold the Socket for one player
 *   - Read messages from the client
 *   - Call gameManager.submitAnswer() when a player answers
 *   - Implement sendMessage() to write to the client's socket
 */
public class PlayerHandler {

    private String nickname;

    public PlayerHandler(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    /**
     * Sends a JSON string to this player's client over the socket.
     * Your teammate implements the real socket writing here.
     */
    public void sendMessage(String json) {
        // stub — teammate implements real socket output
        System.out.println("[stub] -> " + nickname + ": " + json);
    }
}
