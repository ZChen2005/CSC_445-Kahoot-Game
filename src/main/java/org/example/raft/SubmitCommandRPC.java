package org.example.raft;

import java.io.Serializable;

public class SubmitCommandRPC implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String command;

    public SubmitCommandRPC(String command) {this.command = command;}
    public String getCommand() {return command;}
}