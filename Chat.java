/**
 * A Group contains data needed by the server about a group chat. A Chat contains data needed by a client.
 * Keeps track of all Peers.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Chat {

    /**
     * Identifier of the current client.
     */
    public int hostID = 0;

    /**
     * Sequence number of messages sent from current host
     * Goes up by one for each message sent.
     */
    public int sequenceNumber = 0;

    /**
     * Peers currently participating in this group chat
     */
    public ArrayList<Peer> peers = null;

    /**
     * Name of group chat
     */
    public String name = null;

    /**
     * Creates a group chat from the given Group.
     * @param group Data from the Server to initialize a chat among peers.
     */
    public Chat(Group group, int hostID) {
        this.name = group.name;
        this.hostID = hostID;
        this.peers = new ArrayList<Peer>();

        Iterator<User> users = group.users.iterator();

        while (users.hasNext()) {
            User user = users.next();
            this.peers.add(new Peer(user));
        }
    }

    /**
     * Stores all messages, in order to print them out and share them with others.
     * Messages stored in hash table by sender ID, then in an ArrayList of messages indexed by sequence number.
     * Note that there can be null messages in the ArrayList if messages don't arrive in order.
     */
    private HashMap<Integer, ArrayList<Message>> messages = new HashMap<Integer, ArrayList<Message>>();

    /**
     * Searches for the peer with requested ID
     * @param peerID The identifying number for requested peer.
     * @return The Peer with this peerID, or null if that peer doesn't exist
     */
    public Peer checkAddressBook(int peerID) {
        Peer peer = null;
        synchronized (this.peers) {
            Iterator<Peer> peerIterator = this.peers.iterator();
            while (peerIterator.hasNext()) {
                Peer onePeer = peerIterator.next();
                if (onePeer.user.userID == peerID) {
                    peer = onePeer;
                    break;
                }
            }
        }
        return peer;
    }

    /**
     * Notification has arrived that another client has a message.
     * Update the Peer, then possibly request the packet.
     * @param peerID ID number of peer who has the packet
     * @param senderID ID number of person who originally wrote the message.
     * @param sequenceNumber Index in messages written by senderID
     * @param messageCreationDate Date in milliseconds when the message was written.
     */
    public void peerHas(int peerID, int senderID, int sequenceNumber, long messageCreationDate) {
        // find peer
        Peer peer = this.checkAddressBook(peerID);

        if (peer != null) {
            // found peer.
            synchronized (peer) {
                peer.has(senderID, sequenceNumber, messageCreationDate);
            }
        } else {
            System.err.println("Received message from peer with unknown ID "+peerID);
        }
    }

    /**
     * Have fully downloaded / created a new message, so now am free and to send it out.
     * store this message in the chat and publicize its existence to everyone who might care
     * In general, publicize only to people who don't already have it and only those who I might 
     */
    public void have(Message message) {
        // store this message
        synchronized (this.messages) {
            Integer senderKey = new Integer(message.senderID);
            ArrayList<Message> messagesFromSender = this.messages.get(senderKey);
            if (messagesFromSender == null) {
                messagesFromSender = new ArrayList<Message>();
                this.messages.put(senderKey, messagesFromSender);
            }
            // add nulls to pad, so messagesFromSender.get(message.sequenceNumber) will be message.
            while (messagesFromSender.size() < message.sequenceNumber) {
                messagesFromSender.add(null);
            }
            messagesFromSender.add(message);
        }

        // make Control packet for HAVE
        ControlPacket packet = ControlPacket.packetForMessage(message, ControlPacket.Type.HAVE, this.hostID);
        byte[] packetData = packet.pack();

        // send this packet to peers who might be interested.
        synchronized (this.peers) {
            Iterator<Peer> peerIterator = this.peers.iterator();
            while (peerIterator.hasNext()) {
                Peer peer = peerIterator.next();
                if (peer.interestedIn(message)) {
                    peer.sendControlData(packetData);
                }
            }
        }

        // Sending over UDP, so the HAVE message might get lost in the mail.
        // TODO set up a timeout to resend if no one is interested.
    }

    /**
     * A new message has been created and must be sent out
     */
    public void newMessage(String message) {
        this.have(new Message(message.getBytes(StandardCharsets.US_ASCII), this.hostID, this.sequenceNumber++, System.currentTimeMillis()));
    }

    /**
     * Add peer for given user to list of peers.
     * @param user The Peer's credentials
     */
    public void makeFriend(User user) {
        Peer peer = new Peer(user);
        synchronized (this.peers) {
            this.peers.add(peer);
        }
    }
}
