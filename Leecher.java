/**
 * Thread for accepting data packets.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Leecher implements Runnable {
    /**
     * Port for making new connections
     */
    public int port = 0;

    /**
     * Socket for accepting connections.
     */
    private ServerSocket socket = null;

    /**
     * Client, where you put the data.
     */
    private Client client;

    public Leecher(Client client) {
        this.client = client;
        Random rand = new Random();
        while (this.socket == null) {
            try {
                this.socket = new ServerSocket( 1000 + rand.nextInt(200) );
            } catch (Exception ex) {
                // just try again
            }
        }
        this.port = this.socket.getLocalPort();
        
    }

    public void run() {
        
    }

}