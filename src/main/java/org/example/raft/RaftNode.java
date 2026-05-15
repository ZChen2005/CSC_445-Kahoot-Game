package org.example.raft;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class RaftNode {
    private static final int heartbeatMS = 500;
    private static final int minElectionMS = 1500;
    private static final int maxElectionMS = 3000;

    private final int nodeId;
    private final List<PeerInfo> peers;
    private final int clusterSize;

    private volatile NodeRole role = NodeRole.FOLLOWER;
    private volatile int leaderID = -1;

    private final RaftState state = new RaftState();

    private volatile long lastHeartbeatMS = System.currentTimeMillis();
    private volatile long electionTimeoutMs = randomTimeout();

    private final Map<Integer, Integer> nextIndex  = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> matchIndex = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public RaftNode(int nodeId, List<PeerInfo> peers) {
        this.nodeId = nodeId;
        this.peers = peers;
        this.clusterSize = peers.size() + 1;
    }

    public void start() {
        startElectionTimer();
        startHeartbeatTask();
        System.out.println("RaftNode " + nodeId + " started (cluster size " + clusterSize + ")");
    }

    private void startElectionTimer() {
        scheduler.scheduleAtFixedRate(() -> {
            if (role != NodeRole.LEADER && System.currentTimeMillis() - lastHeartbeatMS > electionTimeoutMs) {
                try {
                    startElection();
                } catch (Exception e) {
                    System.err.println("Node " + nodeId + " election error: " + e.getMessage());
                }
            }

        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private synchronized void startElection() throws IOException {
        if (role == NodeRole.LEADER) {return;}

        role = NodeRole.CANDIDATE;
        state.setCurrentTerm(state.getCurrentTerm() + 1);
        state.setVotedFor(nodeId);
        resetElectionTimeout();

        int term = state.getCurrentTerm();
        int lastLogIndex = state.getLastLogIndex();
        int lastLogTerm  = state.getLastLogTerm();
        System.out.println("Node " + nodeId + " starting election – term " + term);

        int votes = 1;
        RequestVoteRPC rpc = new RequestVoteRPC(term, nodeId, lastLogIndex, lastLogTerm);

        for (PeerInfo peer : peers) {
            RequestVoteResponse response = (RequestVoteResponse) RaftClient.sendRPC(peer, rpc);

            if (response == null) {continue;}

            if (response.getTerm() > state.getCurrentTerm()) {
                stepDown(response.getTerm());
                return;
            }

            if (response.isVoteGranted()) {
                votes++;
            }
        }

        if (role == NodeRole.CANDIDATE && votes > clusterSize / 2) {
            becomeLeader();
        } else {
            role = NodeRole.FOLLOWER;
            resetElectionTimeout();
        }
    }

    private synchronized void becomeLeader() {
        role = NodeRole.LEADER;
        leaderID = nodeId;
        System.out.println("Node " + nodeId + " is now LEADER for term " + state.getCurrentTerm());

        int nextIdx = state.getLastLogIndex() + 1;
        for (PeerInfo peer : peers) {
            nextIndex.put(peer.getNodeId(), nextIdx);
            matchIndex.put(peer.getNodeId(), -1);
        }

        sendHeartbeatsToAll();
    }

    private void startHeartbeatTask() {
        scheduler.scheduleAtFixedRate(() -> {
            if (role == NodeRole.LEADER) {
                sendHeartbeatsToAll();
            }
        }, 0, heartbeatMS, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeatsToAll() {
        for (PeerInfo peer : peers) {
            final PeerInfo p = peer;
            scheduler.submit(() -> replicateToPeer(p));
        }
    }

    private void replicateToPeer(PeerInfo peer) {
        while (role == NodeRole.LEADER) {
            AppendEntriesRPC rpc;
            int sentUpTo;

            synchronized (this) {
                int peerId = peer.getNodeId();
                int nextIn = nextIndex.getOrDefault(peerId, 0);
                int prevLogIndex = nextIn - 1;
                int prevLogTerm  = state.getTermAt(prevLogIndex);
                List<LogEntry> entries = state.getEntriesFrom(nextIn);
                sentUpTo = nextIn + entries.size() - 1;

                rpc = new AppendEntriesRPC(state.getCurrentTerm(), nodeId, prevLogIndex, prevLogTerm, entries, state.getCommitIndex());
            }

            AppendEntriesResponse response;
            try {
                response = (AppendEntriesResponse) RaftClient.sendRPC(peer, rpc);
            } catch (IOException e) {
                return;
            }

            if (response == null) return;

            synchronized (this) {
                if (response.getTerm() > state.getCurrentTerm()) {
                    stepDown(response.getTerm());
                    return;
                }

                int peerId = peer.getNodeId();

                if (response.isSuccess()) {
                    if (sentUpTo >= 0) {
                        matchIndex.put(peerId, sentUpTo);
                        nextIndex.put(peerId, sentUpTo + 1);
                        advanceCommitIndex();
                    }
                    return;

                } else {
                    int ni = nextIndex.getOrDefault(peerId, 0);
                    nextIndex.put(peerId, Math.max(0, ni - 1));
                }
            }
        }
    }

    private void advanceCommitIndex() {
        int currentTerm  = state.getCurrentTerm();
        int commitIndex  = state.getCommitIndex();
        int lastLogIndex = state.getLastLogIndex();

        for (int n = lastLogIndex; n > commitIndex; n--) {
            if (state.getTermAt(n) != currentTerm) continue;

            int count = 1;
            for (PeerInfo peer : peers) {
                if (matchIndex.getOrDefault(peer.getNodeId(), -1) >= n) {
                    count++;
                }
            }

            if (count > clusterSize / 2) {
                state.setCommitIndex(n);
                System.out.println("Node " + nodeId + " committed up to index " + n);
                break;
            }
        }
    }

    public synchronized RequestVoteResponse handleRequestVote(RequestVoteRPC rpc) {
        if (rpc.getTerm() > state.getCurrentTerm()) {
            stepDown(rpc.getTerm());
        } else if (rpc.getTerm() < state.getCurrentTerm()) {
            return new RequestVoteResponse(state.getCurrentTerm(), false);
        }

        boolean logOk = rpc.getLastLogTerm() > state.getLastLogTerm() || (rpc.getLastLogTerm() == state.getLastLogTerm() && rpc.getLastLogIndex() >= state.getLastLogIndex());
        Integer votedFor = state.getVotedFor();
        boolean canVote = votedFor == null || votedFor == rpc.getCandidateId();

        if (logOk && canVote) {
            state.setVotedFor(rpc.getCandidateId());
            resetElectionTimeout();
            return new RequestVoteResponse(state.getCurrentTerm(), true);
        }

        return new RequestVoteResponse(state.getCurrentTerm(), false);
    }

    public synchronized AppendEntriesResponse handelAppendEntries(AppendEntriesRPC rpc) {
        if (rpc.getTerm() < state.getCurrentTerm()) {
            return new AppendEntriesResponse(state.getCurrentTerm(), false, state.getLastLogIndex());
        }

        resetElectionTimeout();

        if (rpc.getTerm() > state.getCurrentTerm()) {
            state.setCurrentTerm(rpc.getTerm());
            state.setVotedFor(null);
        }
        role = NodeRole.FOLLOWER;
        leaderID = rpc.getLeaderId();

        int prevLogIndex = rpc.getPrevLogIndex();
        int prevLogTerm  = rpc.getPrevLogTerm();

        if (prevLogIndex >= 0) {
            if (prevLogIndex >= state.getLogSize() || state.getTermAt(prevLogIndex) != prevLogTerm) {
                return new AppendEntriesResponse(state.getCurrentTerm(), false, state.getLastLogIndex());
            }
        }

        List<LogEntry> newEntries = rpc.getEntries();
        int insertAt = prevLogIndex + 1;

        for (int i = 0; i < newEntries.size(); i++) {
            int logIdx = insertAt + i;

            if (logIdx < state.getLogSize()) {
                if (state.getTermAt(logIdx) != newEntries.get(i).getTerm()) {
                    state.truncateFrom(logIdx);
                    state.appendEntry(newEntries.get(i));
                }
            } else {
                state.appendEntry(newEntries.get(i));
            }
        }

        int leaderCommit = rpc.getLeaderCommit();
        if (leaderCommit > state.getCommitIndex()) {
            int newCommit = Math.min(leaderCommit, state.getLastLogIndex());
            state.setCommitIndex(newCommit);
        }

        return new AppendEntriesResponse(state.getCurrentTerm(), true, state.getLastLogIndex());
    }

    public synchronized SubmitCommandResponse submitCommand(String command) {
        if (role != NodeRole.LEADER) {
            String msg = leaderID == -1
                    ? "No leader elected yet, try again shortly"
                    : "Not the leader, redirect to node " + leaderID;
            return new SubmitCommandResponse(false, msg, -1, leaderID);
        }

        LogEntry entry = new LogEntry(state.getCurrentTerm(), command);
        state.appendEntry(entry);
        int index = state.getLastLogIndex();

        System.out.println("Node " + nodeId + " appended command at index " + index + ": " + command);

        for (PeerInfo peer : peers) {
            final PeerInfo p = peer;
            scheduler.submit(() -> replicateToPeer(p));
        }

        return new SubmitCommandResponse(true, "Command accepted at index " + index, index, nodeId);
    }

    public NodeRole getRole() {return role;}
    public int getNodeId() {return nodeId;}
    public int getLeaderId() {return leaderID;}
    public int getCommitIndex() {return state.getCommitIndex();}

    public synchronized List<LogEntry> getCommittedEntries() {
        int commit = state.getCommitIndex();
        List<LogEntry> result = new ArrayList<>();
        for (int i = 0; i <= commit; i++) {
            result.add(state.getEntry(i));
        }
        return result;
    }

    private void stepDown(int newTerm) {
        state.setCurrentTerm(newTerm);
        state.setVotedFor(null);
        role = NodeRole.FOLLOWER;
        resetElectionTimeout();
    }

    private void resetElectionTimeout() {
        lastHeartbeatMS   = System.currentTimeMillis();
        electionTimeoutMs = randomTimeout();
    }

    private static long randomTimeout() {
        return minElectionMS + new Random().nextInt(maxElectionMS - minElectionMS);
    }
}
