package org.example.raft;
import java.io.Serializable;

public class SubmitCommandResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean success;
    private final String message;
    private final int logIndex;
    private final int redirectLeaderId;

    public SubmitCommandResponse(boolean success, String message, int logIndex, int redirectLeaderId) {
        this.success = success;
        this.message = message;
        this.logIndex = logIndex;
        this.redirectLeaderId = redirectLeaderId;
    }

    public boolean isSuccess() {return success;}
    public String getMessage() {return message;}
    public int getLogIndex() {return logIndex;}
    public int getRedirectLeaderId() {return redirectLeaderId;}
}