package org.example.Server;

import com.google.gson.Gson;
import org.example.raft.RaftNode;
import org.example.raft.NodeRole;
import org.example.Shared.GameConfig;
import org.example.Shared.Question;

import java.util.List;
import java.util.concurrent.*;

public class GameManager {

    private final RaftNode       raftNode;
    private final List<Question> questions;
    private final ScoreBoard     scoreBoard;
    private final Gson           gson       = new Gson();
    private final String         myNickname;

    // Answers from ALL players arrive here via GameStateApplier
    private final BlockingQueue<Answer> answerQueue = new LinkedBlockingQueue<>();

    // Collected nicknames from both nodes
    private final java.util.Set<String> registeredNicknames =
            java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());

    public static class Answer {
        public final String nickname;
        public final String letter;
        public final long   elapsedMs;

        public Answer(String nickname, String letter, long elapsedMs) {
            this.nickname  = nickname;
            this.letter    = letter;
            this.elapsedMs = elapsedMs;
        }
    }

    public GameManager(RaftNode raftNode, List<Question> questions, String myNickname) {
        this.raftNode    = raftNode;
        this.questions   = questions;
        this.myNickname  = myNickname;
        this.scoreBoard  = new ScoreBoard();
    }

    // ── Called by GameStateApplier for every ANSWER log entry ──
    // Runs on both nodes — only leader uses it for scoring
    public void receiveAnswer(String nickname, String letter, long elapsedMs) {
        answerQueue.offer(new Answer(nickname, letter, elapsedMs));
    }

    // ── Called by GameStateApplier for every NICKNAME log entry ──
    public void registerNickname(String nickname) {
        registeredNicknames.add(nickname);
        System.out.println("[GameManager] Registered nickname: " + nickname);
    }

    // ── Called by GUI when this player clicks an answer ──
    public void submitAnswer(String letter) {
        String command = "ANSWER:" + myNickname + ":" + letter;
        raftNode.submitCommand(command);
    }

    // ── Main game loop — leader only ──
    public void start(List<String> allNicknames) {
        scoreBoard.registerPlayers(allNicknames);

        submitRaft("START", "Game is starting!");
        sleep(2000);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            System.out.printf("%n[GameManager] Question %d/%d%n", i + 1, questions.size());
            playQuestion(q);

            String scoresJson = gson.toJson(scoreBoard.getRankings());
            submitRaft("SCORE", scoresJson);
            scoreBoard.printStandings();

            if (i < questions.size() - 1) {
                sleep(GameConfig.SCOREBOARD_DISPLAY_SECONDS * 1000L);
            }
        }

        String finalJson = gson.toJson(scoreBoard.getRankings());
        submitRaft("GAME_OVER", finalJson);
        System.out.println("[GameManager] Game over! Winner: " + scoreBoard.getWinner().getNickname());
    }

    private void playQuestion(Question question) {
        answerQueue.clear();

        int expectedPlayers = scoreBoard.getRankings().size();
        java.util.Set<String> answered = new java.util.HashSet<>();

        submitRaft("QUESTION", gson.toJson(question));

        long startTime   = System.currentTimeMillis();
        long timeLimitMs = GameConfig.QUESTION_TIMER_SECONDS * 1000L;
        long endTime     = startTime + timeLimitMs;

        while (System.currentTimeMillis() < endTime && answered.size() < expectedPlayers) {
            long remaining = (endTime - System.currentTimeMillis()) / 1000;
            submitRaft("TIMER", String.valueOf(remaining));

            try {
                Answer answer = answerQueue.poll(1, TimeUnit.SECONDS);
                if (answer != null && !answered.contains(answer.nickname)) {
                    answered.add(answer.nickname);
                    if (question.isCorrect(answer.letter)) {
                        scoreBoard.recordCorrectAnswer(answer.nickname, answer.elapsedMs);
                        System.out.printf("[GameManager] %s CORRECT in %dms%n",
                                answer.nickname, answer.elapsedMs);
                    } else {
                        scoreBoard.recordWrongAnswer(answer.nickname);
                        System.out.printf("[GameManager] %s WRONG (%s)%n",
                                answer.nickname, answer.letter);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        for (ScoreBoard.PlayerScore ps : scoreBoard.getRankings()) {
            if (!answered.contains(ps.getNickname())) {
                scoreBoard.recordWrongAnswer(ps.getNickname());
                System.out.printf("[GameManager] %s timed out%n", ps.getNickname());
            }
        }

        submitRaft("TIMER", "0");
    }

    private void submitRaft(String type, String data) {
        if (raftNode.getRole() != NodeRole.LEADER) return;
        raftNode.submitCommand(type + ":" + data);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public ScoreBoard getScoreBoard() { return scoreBoard; }

    public java.util.Set<String> getRegisteredNicknames() {
        return registeredNicknames;
    }
}
