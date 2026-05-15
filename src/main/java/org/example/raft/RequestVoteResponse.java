package org.example.raft;

import java.io.Serializable;

public class RequestVoteResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int term;
    private final boolean voteGranted;

    public RequestVoteResponse(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() {return term;}
    public boolean isVoteGranted() {return voteGranted;}
}
