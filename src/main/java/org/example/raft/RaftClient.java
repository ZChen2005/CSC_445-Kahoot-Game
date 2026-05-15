package org.example.raft;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class RaftClient {

    public static Object sendRPC(PeerInfo peer, Object rpc) throws IOException {
        try (
                Socket socket = new Socket(peer.getHost(), peer.getPort());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ) {
            out.writeObject(rpc);
            out.flush();

            try(ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                return in.readObject();
            } catch (IOException e) {
                throw e;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
