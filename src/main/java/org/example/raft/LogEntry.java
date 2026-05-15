package org.example.raft;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int term;
    private final String command;

    public LogEntry(int term, String command) {
        this.term = term;
        this.command = command;
    }

    public int getTerm() {
        return term;
    }
    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return  "[" + term + "]" + command;
    }
}