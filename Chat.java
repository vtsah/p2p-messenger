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
     * Reference to client which is in this chat.
     */
    public Client client = null;

    /**
     * The number of unchoke slots open.
     * Start off with 4. They go up and down as slots are taken and released.
     * Should never go negative.
     */
    public int unchokeSlotsAvailable = 4;

    /**
     * Creates a group chat from the given Group.
     * @param group Data from the Server to initialize a chat among peers.
     */
    public Chat(Client client, Group group, int hostID) {
        this.client = client;
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

        if (peer == null) {
            System.err.println("Received message from peer with unknown ID "+peerID);
            return;
        }

        // found peer.
        peer.has(senderID, sequenceNumber, messageCreationDate);

        // do I want this message? should I ask for it?
        Message myVersion = this.getMessage(senderID, sequenceNumber);

        if (myVersion == null) {
            // ask for it
            ControlPacket interest = new ControlPacket(ControlPacket.Type.INTERESTED, this.hostID, senderID, sequenceNumber, messageCreationDate);
            peer.sendControlData(interest.pack());
        }
    }

    /**
     * Someone is interested in something I have. Check if I actually have it and if I have UNCHOKE spots open.
     * @param peerID The ID for the peer who is interested
     * @param messageCreator The ID for the original creator of the message
     * @param sequenceNumber The sequence number of the message
     * @param messageCreationDate For sending response, not used directly.
     */
    public void peerIsInterested(int peerID, int messageCreator, int sequenceNumber, long messageCreationDate) {
        // find peer
        Peer peer = this.checkAddressBook(peerID);

        if (peer == null) {
            System.err.println("Received message from peer with unknown ID "+peerID);
            return;
        }

        // Do I have this message?
        Message myVersion = this.getMessage(messageCreator, sequenceNumber);

        if (myVersion == null) {
            return; // someone asked for something I don't have
        }

        boolean shouldUnchoke = false;
        synchronized (this) {
            synchronized (peer) {
                peer.chokedByMe = (this.unchokeSlotsAvailable == 0);
            }
            if (this.unchokeSlotsAvailable > 0) {
                this.unchokeSlotsAvailable--;
                shouldUnchoke = true;
            }
        }

        // TODO be more picky about unchoking: tit-for-tat

        ControlPacket packet = new ControlPacket(shouldUnchoke ? ControlPacket.Type.UNCHOKE : ControlPacket.Type.CHOKE, this.hostID, messageCreator, sequenceNumber, messageCreationDate);

        peer.sendControlData(packet.pack());
    }

    /**
     * Retrieves a message.
     * @return Message with given sender ID and sequence number or null if doesn't exist.
     * If currently being downloaded, will return non-null message with null data.
     */
    public Message getMessage(int senderID, int sequenceNumber) {
        Message message = null;
        synchronized (this.messages) {
            ArrayList<Message> messagesFromSender = this.messages.get(new Integer(senderID));
            if (messagesFromSender == null || sequenceNumber >= messagesFromSender.size()) {
                return null;
            }
            message = messagesFromSender.get(sequenceNumber);
        }
        return message;
    }

    /**
     * Stores a message in the messages data structure
     */
    public void storeMessage(Message message) {
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
    }

    /**
     * Print the message? Only if it's the next in line.
     */
    public boolean shouldPrintMessage(Message message) {
        // TODO: print them in order
        return message.senderID != this.hostID; // don't reprint the ones the client is sending.
    }

    /**
     * Have fully downloaded / created a new message, so now am free and to send it out.
     * store this message in the chat and publicize its existence to everyone who might care
     * In general, publicize only to people who don't already have it and only those who I might 
     */
    public void have(Message message) {
        this.storeMessage(message);

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

        if (this.shouldPrintMessage(message)) {
            // who sent this?
            Peer sender = this.checkAddressBook(message.senderID);

            if (sender == null) {
                System.err.println("Received message written by a rando");
            }
            // yeah, but what's his name?
            String name = sender.user.username;

            // and what was the message again?
            String text = new String(message.data, StandardCharsets.US_ASCII);

            System.out.println(name+": "+text);
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

    /**
     * Notification that the peer has choked this client.
     * Make sure not to send requests to this peer until unchoked.
     */
    public void peerChoked(int peerID) {
        Peer peer = this.checkAddressBook(peerID);
        if (peer != null) {
            synchronized (peer) {
                peer.chokedMe = true;
            }
        }
    }

    /**
     * Notification that the peer has unchoked this client, due to a request for the specified message
     */
    public void peerUnchoked(int peerID, int messageCreator, int sequenceNumber, long messageCreationDate) {
        Peer peer = this.checkAddressBook(peerID);
        if (peer != null) {
            synchronized (peer) {
                peer.chokedMe = false;
            }
            // check if I want this one
            Message myVersion = this.getMessage(messageCreator, sequenceNumber);

            if (myVersion == null) {
                // occupy the spot for the message, so I don't try to download it twice.
                this.storeMessage(new Message(null, messageCreator, sequenceNumber, messageCreationDate));

                // request this packet.
                // should definitely do the request in another thread,
                // because don't want to block the control packet thread with a TCP client
                Leecher leecher = new Leecher(peer, this, messageCreator, sequenceNumber, messageCreationDate);
                Thread leecherThread = new Thread(leecher);

                leecherThread.start();
            } else {
                // TODO request another from this peer, since I've been unchoked I can request any.

                // send back cancel
                ControlPacket cancelPacket = ControlPacket.packetForMessage(myVersion, ControlPacket.Type.CANCEL, this.hostID);
            }
        }
    }
}
