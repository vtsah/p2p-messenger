/**
 * Thread / ServerSocket for getting request for, and sending data packets.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Seeder implements Runnable {
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

    public Seeder(Client client) {
        this.client = client;
        Random rand = new Random();
        while (this.socket == null) {
            try {
                // apparently you need to initialize with a port number.
                // just get one that works.
                this.socket = new ServerSocket( 1000 + rand.nextInt(1000) );
            } catch (Exception ex) {
                // just try again
            }
        }
        this.port = this.socket.getLocalPort();
        
    }

    public void run() {

    }

}