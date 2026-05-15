package org.example.raft;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RaftServer {
    private final RaftNode node;
    private final int port;

    public RaftServer(RaftNode node, int port) {
        this.node = node;
        this.port = port;
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("RaftServer for node " + node.getNodeId() + " listening on port " + port);

                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handleConnection(socket)).start();
                }
            } catch (Exception e) {
                System.err.println("RaftServer fatal error: " + e.getMessage());
                e.printStackTrace();
            }
        },"raftServer-" + node.getNodeId()).start();
    }

    private void handleConnection(Socket socket) {
        try (
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
        ) {
            Object request = in.readObject();
            Object response = null;
            
            if (request instanceof RequestVoteRPC) {
                response = node.handleRequestVote((RequestVoteRPC) request);
            } else if (request instanceof AppendEntriesRPC) {
                response = node.handelAppendEntries((AppendEntriesRPC) request);
            } else if (request instanceof SubmitCommandRPC) {
                response = node.submitCommand(((SubmitCommandRPC) request).getCommand());
            }

            out.writeObject(response);
            out.flush();
        } catch (Exception e) {
            System.err.println("Node " + node.getNodeId() + " error handling connection: " + e.getMessage());
        }
    }
}