/**
 * While a User just contains information carried by the Server about each user (name and connection instructions),
 * A Peer contains information carried by a client about another client.
 * A Peer object also has methods to send data and control packets to the peer and to respond to them.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Peer {
    /**
     * Basic connectivity information, and username
     */
    public User user;

    /**
     * Set of messages this peer has.
     * Maps message creator ID -> (set of sequence numbers)
     */
    public HashMap<Integer, HashSet<Integer>> messages = new HashMap<Integer, HashSet<Integer>>();

    public Peer(User user) {
        this.user = user;
    }

    /**
     * @param message Message which could possibly request.
     * @return Boolean of whether this peer is interested (doesn't have it already).
     */
    public boolean interestedIn(Message message) {
        synchronized (this.messages) {
            HashSet<Integer> messagesFromSender = this.messages.get(new Integer(message.senderID));
            if (messagesFromSender == null) {
                return true;
            }
            return !messagesFromSender.contains(new Integer(message.sequenceNumber));
        }
    }

    /**
     * Sends a control packet over UDP.
     * @param data The control packet to send
     */
    public void sendControlData(byte[] data) {

        // first create socket to send packet
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, this.user.address, this.user.port);

        try {
            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Have received notification that Peer has a new packet that I can request.
     * @param message The message in question that the peer now has.
     * @return boolean: false if Peer already knew about packet, true if this is new information
     */
    public boolean has(Message message) {
        synchronized (this.messages) {
            HashSet<Integer> messagesFromSender = this.messages.get(new Integer(message.senderID));
            if (messagesFromSender == null) {
                messagesFromSender = new HashSet<Integer>();
                this.messages.put(new Integer(message.senderID), messagesFromSender);
            }

            if (!messagesFromSender.contains(new Integer(message.sequenceNumber))) {
                messagesFromSender.add(new Integer(message.sequenceNumber));
                return true;
            }
        }

        // TODO store and use message creation date

        return false;
    }

    /**
     * Send my contact information to this peer.
     * @param user The contact information to send.
     */
    public void giveBusinessCard(User user) {
        // System.out.println("Sending business card to "+this.user.username);
        // construct business card
        byte[] card = user.pack();

        Socket socket;
        DataOutputStream outToServer;
        try {
            // System.out.println("Sending business card to IP "+this.user.address);
            socket = new Socket(this.user.address, this.user.dataPort);
            outToServer = new DataOutputStream(socket.getOutputStream());
            // no input necessary, this is a one-way conversation

            outToServer.write(card);
            outToServer.flush(); // necessary?

            socket.close();
        } catch (IOException ex) {
            //ex.printStackTrace();
            return;
        }
    }

    /**
     * Keep track of whether this peer has choked or unchoked the current client,
     * per the https://wiki.theory.org/BitTorrentSpecification
     */
    public boolean chokedMe = true;

    /**
     * Keep track of whether this peer is choked or unchoked from the point of view of the current client.
     */
    public boolean chokedByMe = true;

    /**
     * Keep track of whether I'm currently making a request to this peer.
     * Never make more than one request at the same time to the same peer, because they will probably be refused.
     */
    public boolean currentlyRequesting = false;
}

