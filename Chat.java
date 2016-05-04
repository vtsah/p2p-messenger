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
     * Maximum number of unchoked peers
     */
    public final int MAX_UNCHOKE = 4;

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
     * The index of blocks of data I've sent.
     */
    public int blockIndex = 0;

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
     * Set of unchoked peers. This should never get bigger than 4.
     */
    public HashSet<Peer> unchokedPeers = new HashSet<Peer>();

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
     * @param message The message metadata in question.
     */
    public void peerHas(int peerID, Message message) {
        // find peer
        Peer peer = this.checkAddressBook(peerID);

        if (peer == null) {
            System.err.println("Received message from peer with unknown ID "+peerID);
            return;
        }

        // found peer.
        if (!peer.has(message)) {
            // the peer already knew about this one. don't want to keep receiving these, so notify
            ControlPacket annoyedResponse = new ControlPacket(ControlPacket.Type.HAVE, this.hostID, message);
            peer.sendControlData(annoyedResponse.pack());
            return;
        }

        // do I want this message? should I ask for it?
        Message myVersion = this.getMessage(message.senderID, message.sequenceNumber);

        if (myVersion == null) {
            // ask for it
            ControlPacket interest = new ControlPacket(ControlPacket.Type.INTERESTED, this.hostID, message);
            peer.sendControlData(interest.pack());
        }
    }

    /**
     * Someone is interested in something I have. Check if I actually have it and if I have UNCHOKE spots open.
     * @param peerID The ID for the peer who is interested
     * @param messageCreator The message the peer is interested in.
     */
    public void peerIsInterested(int peerID, Message message) {
        // find peer
        Peer peer = this.checkAddressBook(peerID);

        if (peer == null) {
            System.err.println("Received message from peer with unknown ID "+peerID);
            return;
        }

        synchronized (peer) {
            if (!peer.chokedByMe) {
                peer.sendControlData(new ControlPacket(ControlPacket.Type.UNCHOKE, this.hostID, null).pack());
                return;
            }
        }

        // Do I have this message?
        Message myVersion = this.getMessage(message.senderID, message.sequenceNumber);

        if (myVersion == null) {
            return; // someone asked for something I don't have
        }

        boolean shouldUnchoke = false;
        synchronized (this.unchokedPeers) {
            if (this.unchokedPeers.size() < MAX_UNCHOKE) {
                synchronized (peer) {
                    peer.chokedByMe = false;
                }
                this.unchokedPeers.add(peer);
                shouldUnchoke = true;
            }
        }

        // TODO be more picky about unchoking: tit-for-tat

        ControlPacket packet = new ControlPacket(shouldUnchoke ? ControlPacket.Type.UNCHOKE : ControlPacket.Type.CHOKE, this.hostID, null);

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
     * Stores a message in the messages data structure.
     * Based on the message sender and sequence number.
     * Does not assume that the message doesn't already exist or that it should go at the end.
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
            while (messagesFromSender.size() <= message.sequenceNumber) {
                messagesFromSender.add(null);
            }
            messagesFromSender.set(message.sequenceNumber, message);
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
     * @param message The Message I now have.
     * @param careOf The Peer I received this message through (the id of the actual person who sent it to me)
     */
    public void have(Message message, int careOf) {
        this.storeMessage(message);

        // make Control packet for HAVE
        ControlPacket packet = new ControlPacket(ControlPacket.Type.HAVE, this.hostID, message);
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
            String sender = this.whatsHisName(message.senderID);

            // and what was the message again?
            String text = new String(message.data, StandardCharsets.US_ASCII);

            if (message.senderID != careOf) {
                String goBetween = this.whatsHisName(careOf);

                System.out.println(sender+" (via "+goBetween+"): "+text);
            } else {
                System.out.println(sender+": "+text);
            }
        }

        // Sending over UDP, so the HAVE message might get lost in the mail.
        // TODO set up a timeout to resend if no one is interested.
    }

    /**
     * A new block has been written and must be sent out
     */
    public void newBlock(String block) {
        this.newBlock(block.getBytes(StandardCharsets.US_ASCII), this.blockIndex++);
    }

    /**
     * Send out a bunch of bytes, to everyone.
     */
    public void newBlock(byte[] block, int blockIndex) {
        // break up blocks into little pieces.

        int pieceCount = (block.length + Message.MAX_PIECE - 1) / Message.MAX_PIECE;

        for (int i = 0; i < pieceCount; i++) {
            int startIndex = i * Message.MAX_PIECE;
            int bytesRemaining = block.length - startIndex;
            if (bytesRemaining > Message.MAX_PIECE) {
                bytesRemaining = Message.MAX_PIECE;
            }
            byte[] piece = Arrays.copyOfRange(block, startIndex, startIndex + bytesRemaining);
            this.newPiece(piece, blockIndex);
        }
    }

    public void newPiece(byte[] piece, int blockIndex) {
        this.have(new Message(piece, this.hostID, blockIndex, this.sequenceNumber++, System.currentTimeMillis()), this.hostID);
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
     * Looks up someone's name by ID number.
     * @return The Peer's name, or "rando" if peer isn't in known peers.
     */
    public String whatsHisName(int userID) {
        // hey it's me! I should know my own name.
        if (userID == this.hostID) {
            return this.client.user.username;
        }
        Peer peer = this.checkAddressBook(userID);
        if (peer == null) {
            // this really shouldn't happen
            System.err.println("Couldn't find someone's name");
            return "rando";
        }
        return peer.user.username;
    }

    /**
     * Notification that the peer has unchoked this client, due to a request for the specified message
     */
    public void peerUnchoked(int peerID) {
        Peer peer = this.checkAddressBook(peerID);
        if (peer != null) {

            int sequenceNumber = -1;
            int messageCreator = -1;

            synchronized (peer) {
                if (peer.currentlyRequesting) {
                    // don't request another.
                    return;
                }
                peer.chokedMe = false;

                synchronized (peer.messages) {

                    // loop through peer's messages to find one I want.
                    Iterator<Integer> peerMessageSenders = peer.messages.keySet().iterator();
                    while (peerMessageSenders.hasNext()) {
                        Integer peerMessageSender = peerMessageSenders.next();

                        Iterator<Integer> sequenceNumbers = peer.messages.get(peerMessageSender).iterator();
                        while (sequenceNumbers.hasNext()) {
                            int availableSequenceNumber = sequenceNumbers.next().intValue();

                            Message myVersion = this.getMessage(peerMessageSender.intValue(), availableSequenceNumber);

                            // I don't have this one, so I want it.
                            if (myVersion == null) {
                                sequenceNumber = availableSequenceNumber;
                                messageCreator = peerMessageSender.intValue();
                                break;
                            }
                        }
                        if (messageCreator >= 0) {
                            break;
                        }
                    }
                }
            }

            if (messageCreator >= 0) {
                // occupy the spot for the message, so I don't try to download it twice.
                this.storeMessage(new Message(null, messageCreator, 0, sequenceNumber, 0));

                // request this packet.
                // should definitely do the request in another thread,
                // because don't want to block the control packet thread with a TCP client
                Leecher leecher = new Leecher(peer, this, messageCreator, sequenceNumber);
                Thread leecherThread = new Thread(leecher);

                leecherThread.start();
            } else {
                // send back cancel
                ControlPacket cancelPacket = new ControlPacket(ControlPacket.Type.CANCEL, this.hostID, null);
                peer.sendControlData(cancelPacket.pack());
            }
        }
    }

    /**
     * Notification that the peer has canceled a request for unchoking.
     * Basically, I should choke this peer.
     */
    public void peerCanceled(int peerID) {
        Peer peer = this.checkAddressBook(peerID);
        if (peer != null) {
            synchronized (peer) {
                if (!peer.chokedByMe) {
                    synchronized (this.unchokedPeers) {
                        this.unchokedPeers.remove(peer);
                    }
                }
                peer.chokedByMe = true;
            }
        }
    }
}
