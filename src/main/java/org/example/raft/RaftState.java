package org.example.raft;

import java.util.ArrayList;
import java.util.List;

public class RaftState {
    private int currentTerm = 0;
    private Integer votedFor = null;
    private final List<LogEntry> log = new ArrayList<>();
    private int commitIndex = -1;
    private int lastApplied = -1;

    public synchronized int getCurrentTerm() {return currentTerm;}
    public synchronized void setCurrentTerm(int term) {this.currentTerm = term;}
    public synchronized Integer getVotedFor() {return votedFor;}
    public synchronized void setVotedFor(Integer votedFor) {this.votedFor = votedFor;}
    public synchronized int getLogSize() {return log.size();}
    public synchronized int getLastLogIndex() {return log.size() - 1;}
    public synchronized int getLastLogTerm() {return log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm();}

    public synchronized int getTermAt(int index) {
        if (index < 0 || index >= log.size()){return 0;}
        return log.get(index).getTerm();
    }

    public synchronized LogEntry getEntry(int index) {return log.get(index);}

    public synchronized List<LogEntry> getEntriesFrom(int fromIndex) {
        if (fromIndex >= log.size()) return new ArrayList<>();
        return new ArrayList<>(log.subList(fromIndex, log.size()));
    }

    public synchronized void appendEntry(LogEntry entry) {log.add(entry);}

    public synchronized void truncateFrom(int index) {
        while (log.size() > index) {
            log.remove(log.size() - 1);
        }
    }

    public synchronized void appendIfMissing(int index, LogEntry entry) {
        if (index < log.size()) return; // already there
        log.add(entry);
    }

    public synchronized int getCommitIndex() {return commitIndex;}
    public synchronized void setCommitIndex(int idx) {this.commitIndex = idx;}
    public synchronized int getLastApplied() {return lastApplied;}
    public synchronized void setLastApplied(int idx) {this.lastApplied = idx;}
}
