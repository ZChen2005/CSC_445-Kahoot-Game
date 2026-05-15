package org.example;

import org.example.Client.ClientGUI;
import org.example.Server.GameManager;
import org.example.Server.GameStateApplier;
import org.example.Server.QuestionLoader;
import org.example.Server.ScoreBoard;
import org.example.raft.NodeRole;
import org.example.raft.PeerInfo;
import org.example.raft.RaftNode;
import org.example.raft.RaftServer;
import org.example.Shared.Question;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static final List<PeerInfo> ALL_NODES = Arrays.asList(
            new PeerInfo(0, "localhost", 26930),
            new PeerInfo(1, "localhost", 26931)
    );

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.err.println("Usage: java org.example.Main <nodeId> <myNickname> <otherNickname>");
            System.exit(1);
        }

        int    nodeId        = Integer.parseInt(args[0]);
        String myNickname    = args[1];
        String otherNickname = args[2];

        PeerInfo self = ALL_NODES.get(nodeId);
        List<PeerInfo> peers = ALL_NODES.stream()
                .filter(p -> p.getNodeId() != nodeId)
                .collect(Collectors.toList());

        System.out.println("[Main] Starting node " + nodeId + " as " + myNickname);

        RaftNode   raftNode   = new RaftNode(nodeId, peers);
        RaftServer raftServer = new RaftServer(raftNode, self.getPort());
        raftServer.start();
        raftNode.start();

        List<Question> questions = QuestionLoader.load();

        GameManager gameManager = new GameManager(raftNode, questions, myNickname);
        ClientGUI   gui         = new ClientGUI(myNickname, gameManager);

        gameManager.registerNickname(myNickname);
        gameManager.registerNickname(otherNickname);

        ScoreBoard       scoreBoard = gameManager.getScoreBoard();
        GameStateApplier applier    = new GameStateApplier(raftNode, gui, scoreBoard, gameManager);
        Thread applierThread = new Thread(applier, "state-applier-" + nodeId);
        applierThread.setDaemon(true);
        applierThread.start();

        gui.showWaiting("Waiting for Raft leader election...");
        gui.show();

        System.out.println("[Main] Waiting for leader election...");
        waitForCluster(raftNode);

        if (raftNode.getRole() == NodeRole.LEADER) {
            System.out.println("[Main] This node is LEADER — starting game!");
            gui.showWaiting("You are the host! Game starting soon...");
            Thread.sleep(2000);

            List<String> allNicknames = new java.util.ArrayList<>(
                    gameManager.getRegisteredNicknames());
            System.out.println("[Main] Starting game with: " + allNicknames);

            new Thread(() -> gameManager.start(allNicknames), "game-loop").start();
        } else {
            System.out.println("[Main] This node is FOLLOWER — waiting for game...");
            gui.showWaiting("Connected! Waiting for game to start...");
        }

        Thread.currentThread().join();
    }

    private static void waitForCluster(RaftNode raftNode) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (raftNode.getLeaderId() != -1) {
                System.out.println("[Main] Leader elected: node " + raftNode.getLeaderId());
                return;
            }
            Thread.sleep(300);
        }
        System.err.println("[Main] Warning: no leader elected after 30s");
    }
}
