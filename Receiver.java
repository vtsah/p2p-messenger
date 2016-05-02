/**
 * Client's thread to receive control messages over UDP, and handle them.
 * For example, when receives an UNCHOKE, gets data over TCP.
 * When receives a BITFIELD or HAVE message, will update the ClientData.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Receiver implements Runnable {

    /**
     * Port for UDP control packet receiving.
     */
    public int port = 0;

    /**
     * Global data for use on client is stored within Client object.
     */
    private Client client = null;

    /**
     * Socket for receiving control messages
     */
    private DatagramSocket socket = null;

    /**
     * Creates a receiver with given data, and opens up a UDP socket for control messages.
     * The port of this UDP socket is available through the port property.
     * @param data the global data for the client.
     */
    public Receiver(Client client) {
        this.client = client;
        try {
            this.socket = new DatagramSocket();
        } catch (Exception ex) {
            ex.printStackTrace();
            this.socket = null;
        }
        
        this.port = socket.getLocalPort();
    }

    /**
     * Looks up the name of a peer by ID
     */
    public String whatsHisName(int peerID) {
        return this.client.chat.whatsHisName(peerID);
    }

    /**
     * Begin running the thread.
     * Starts using UDP socket for sending and receiving control messages.
     */
    public void run() {

        // Processing loop.
        while (true) {
            
            // Create a datagram packet to hold incomming UDP packet.
            DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
            
            try {
                // Block until receives a UDP packet.
                this.socket.receive(request);
            } catch (IOException ex) {
                ex.printStackTrace();
                continue;
            }
            
            ControlPacket packet = ControlPacket.unpack(request.getData());

            if (packet != null) {
                switch (packet.type) {
                    case HAVE:
                    // System.out.println(this.whatsHisName(packet.senderID)+" has packet made by "+this.whatsHisName(packet.messageCreator)+" with sequence number "+packet.sequenceNumber);
                    this.client.chat.peerHas(packet.senderID, packet.messageCreator, packet.sequenceNumber, packet.messageCreationDate);
                    break;

                    case INTERESTED:
                    // System.out.println(this.whatsHisName(packet.senderID)+" is interested in packet made by "+this.whatsHisName(packet.messageCreator)+" with sequence number "+packet.sequenceNumber);
                    this.client.chat.peerIsInterested(packet.senderID, packet.messageCreator, packet.sequenceNumber, packet.messageCreationDate);
                    break;

                    case CHOKE:
                    // System.out.println(this.whatsHisName(packet.senderID)+" choked me");
                    this.client.chat.peerChoked(packet.senderID); // notification that no requests will be answered until unchoked.
                    break;

                    case UNCHOKE:
                    // System.out.println(this.whatsHisName(packet.senderID)+" unchoked me");
                    this.client.chat.peerUnchoked(packet.senderID, packet.messageCreator, packet.sequenceNumber, packet.messageCreationDate);
                    break;

                    default:
                    System.err.println("Unrecognized packet type "+packet.type);
                    break;
                }
            } else {
                System.err.println("Invalid control packet");
            }
        }
    }

}
