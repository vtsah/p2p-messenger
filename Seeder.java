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
                this.socket = new ServerSocket( 2000 + rand.nextInt(5000), 4 );
            } catch (Exception ex) {
                // just try again
            }
        }
        this.port = this.socket.getLocalPort();
    }

    /**
     * Run thread as sequential server, accepting connections and responding to them.
     * Two kinds of connections: requests for data (make sure sender was unchoked), and business cards (new peer contact info)
     */
    public void run() {

        while (true) {
            // accept connection from connection queue
            Socket connectionSocket;
            BufferedInputStream inFromClient;
            DataOutputStream outToClient;

            int userID;

            try {
                connectionSocket = this.socket.accept();

                // create read stream to get input
                inFromClient = new BufferedInputStream(connectionSocket.getInputStream());
                // create write stream to send output
                outToClient = new DataOutputStream(connectionSocket.getOutputStream());

                // the first 4 bytes will be the user ID of the peer sending the request (for both types of connections)
                userID = IOHelper.getInt(inFromClient);
            } catch (IOException ex) {
                ex.printStackTrace();
                continue;
            }

            // do I recognize this peer ID?
            Peer connectedPeer = this.client.chat.checkAddressBook(userID);

            if (connectedPeer == null) {
                // this is a business card
                User card = User.unpackWithID(userID, inFromClient);

                this.client.chat.makeFriend(card);

                System.out.println(card.username+" has joined the chat");
            } else {
                System.err.println("Received redundant business card from "+connectedPeer);
            }
                

            // all done
            try {
                connectionSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
