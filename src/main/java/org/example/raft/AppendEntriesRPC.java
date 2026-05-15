package org.example.raft;
import java.io.Serializable;
import java.util.List;

public class AppendEntriesRPC implements Serializable {
    private final int term;
    private final int leaderId;
    private final int prevLogIndex;
    private final int prevLogTerm;
    private final List<LogEntry> entries;
    private final int leaderCommit;

    public AppendEntriesRPC(int term, int leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    public int getTerm() {return term;}
    public int getLeaderId() {return leaderId;}
    public int getPrevLogIndex() {return prevLogIndex;}
    public int getPrevLogTerm() {return prevLogTerm;}
    public List<LogEntry> getEntries() {return entries;}
    public int getLeaderCommit()  {return leaderCommit;}
}