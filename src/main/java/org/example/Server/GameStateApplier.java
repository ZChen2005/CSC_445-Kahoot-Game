package org.example.Server;

import com.google.gson.Gson;
import org.example.Client.ClientGUI;
import org.example.raft.LogEntry;
import org.example.raft.RaftNode;

import java.util.List;

public class GameStateApplier implements Runnable {

    private final RaftNode   raftNode;
    private final ClientGUI  gui;
    private final ScoreBoard scoreBoard;
    private final Gson       gson = new Gson();
    private final GameManager gameManager;

    private int  lastApplied      = -1;
    private long questionStartTime = 0;

    public GameStateApplier(RaftNode raftNode, ClientGUI gui,
                            ScoreBoard scoreBoard, GameManager gameManager) {
        this.raftNode    = raftNode;
        this.gui         = gui;
        this.scoreBoard  = scoreBoard;
        this.gameManager = gameManager;
    }

    @Override
    public void run() {
        System.out.println("[GameStateApplier] Started watching Raft log...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int commitIndex = raftNode.getCommitIndex();
                if (commitIndex > lastApplied) {
                    List<LogEntry> entries = raftNode.getCommittedEntries();
                    for (int i = lastApplied + 1; i <= commitIndex && i < entries.size(); i++) {
                        applyEntry(entries.get(i));
                        lastApplied = i;
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[GameStateApplier] Error: " + e.getMessage());
            }
        }
    }

    private void applyEntry(LogEntry entry) {
        String command  = entry.getCommand();
        int    colonIdx = command.indexOf(':');
        if (colonIdx == -1) return;

        String type = command.substring(0, colonIdx).trim();
        String data = command.substring(colonIdx + 1).trim();

        System.out.println("[GameStateApplier] Applying: " + type);

        switch (type) {
            case "START" -> gui.showWaiting(data);

            case "QUESTION" -> {
                questionStartTime = System.currentTimeMillis();
                gui.showQuestion(data);
            }

            case "TIMER" -> gui.updateTimer(data);

            case "ANSWER" -> {
                // Format: ANSWER:nickname:letter
                // Forward to GameManager so it scores regardless of which node answered
                String[] parts = data.split(":", 2);
                if (parts.length == 2) {
                    String nickname = parts[0];
                    String letter   = parts[1];
                    long   elapsed  = System.currentTimeMillis() - questionStartTime;
                    gameManager.receiveAnswer(nickname, letter, elapsed);
                }
            }

            case "NICKNAME" -> {
                // Format: NICKNAME:nodeId:nickname
                String[] parts = data.split(":", 2);
                if (parts.length == 2) {
                    String nickname = parts[1];
                    gameManager.registerNickname(nickname);
                }
            }

            case "SCORE"     -> gui.showScoreboard(data);
            case "GAME_OVER" -> gui.showGameOver(data);
            default -> System.out.println("[GameStateApplier] Unknown: " + type);
        }
    }
}
