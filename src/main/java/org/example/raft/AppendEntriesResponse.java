package org.example.raft;

import java.io.Serializable;

public class AppendEntriesResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int term;
    private final boolean success;
    private final int matchIndex;

    public AppendEntriesResponse(int term, boolean success, int matchIndex) {
        this.term = term;
        this.success = success;
        this.matchIndex = matchIndex;
    }

    public int getTerm() {return term;}
    public boolean isSuccess() {return success;}
    public int getMatchIndex() {return matchIndex;}
}