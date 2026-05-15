package org.example.raft;

import java.io.Serializable;

public class PeerInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int nodeId;
    private final String host;
    private final int port;

    public PeerInfo(int nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    public int getNodeId() {return nodeId;}
    public String getHost() {return host;}
    public int getPort() {return port;}

    @Override
    public String toString() {
        return "node" + nodeId + "@" + host + ":" + port;
    }
}
