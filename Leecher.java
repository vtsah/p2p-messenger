/**
 * Thread / Server (TCP client) for downloading messages.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Leecher implements Runnable {

    /**
     * The destination for the request
     */
    public Peer peer = null;

    /**
     * The place to put the message when you're done with the request
     */
    public Chat chat = null;

    /**
     * The ID of the person who wrote the message
     */
    public int messageCreator = 0;

    /** 
     * The index in which the writer wrote this message
     */
    public int sequenceNumber = 0;

    /**
     * Creates leecher, preparing to request and save a message
     * @param peer Where to request the message
     * @param chat Where to put the message
     * @param messageCreater Who wrote the message
     * @param sequenceNumber Which one of his messages you want.
     */
    public Leecher(Peer peer, Chat chat, int messageCreator, int sequenceNumber) {
        this.peer = peer;
        this.chat = chat;
        this.messageCreator = messageCreator;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Runs thread, which connects to peer and request a message.
     */
    public void run() {
        synchronized (this.peer) {
            if (this.peer.currentlyRequesting) {
                return;
            }
            this.peer.currentlyRequesting = true;
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        // construct the request
        // first put the current client
        IOHelper.writeInt(this.chat.hostID, byteStream);

        // now put the message request
        IOHelper.writeInt(messageCreator, byteStream);
        IOHelper.writeInt(sequenceNumber, byteStream);

        byte[] request = byteStream.toByteArray();

        Socket socket;
        DataOutputStream outToServer;
        BufferedInputStream inFromServer;
        try {
            socket = new Socket(this.peer.user.address, this.peer.user.dataPort);
            outToServer = new DataOutputStream(socket.getOutputStream());
            inFromServer = new BufferedInputStream(socket.getInputStream());

            outToServer.write(request);
            outToServer.flush(); // necessary?
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        Message receivedMessage = Message.unpack(inFromServer);
        this.chat.have(receivedMessage, this.peer.user.userID);

        try {
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        synchronized (this.peer) {
            this.peer.currentlyRequesting = false;
        }
    }
}
