package org.example.raft;

import java.io.Serializable;

public class RequestVoteRPC implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int term;
    private final int candidateId;
    private final int lastLogIndex;
    private final int lastLogTerm;

    public RequestVoteRPC(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public int getTerm() {return term;}
    public int getCandidateId() {return candidateId;}
    public int getLastLogIndex() {return lastLogIndex;}
    public int getLastLogTerm()  {return lastLogTerm;}
}
