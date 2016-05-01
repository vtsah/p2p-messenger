/**
 * Packs and unpacks control packets. All meta-communication between clients is done with control packets; messages are sent separately.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class ControlPacket {

    // see https://wiki.theory.org/BitTorrentSpecification
    public enum Type {
        HAVE, // send when have just successfully received / downloaded a single message
        CHOKE, // don't make any requests
        UNCHOKE, // allowing a request to be made.
        INTERESTED, // send when you are interested in a certain packet
        KEEPALIVE, // send lots of these messages to make sure peers are still alive.
        // KEEPALIVE can also be used to remind people how many packets the sender has actually sent
    };

    /**
     * Unpacks a control packet from an array of bytes received through UDP.
     * @param data the array of bytes received in a control packet sent over the wire
     * @return An initialized ControlPacket representing all of the data
     */
    public static ControlPacket unpack(byte[] data) {

        Type type;
        int senderID, messageCreator, sequenceNumber;
        long messageCreationDate;

        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
            BufferedInputStream input = new BufferedInputStream(byteStream);

            type = Type.values()[IOHelper.getInt(input)];
            senderID = IOHelper.getInt(input);
            messageCreator = IOHelper.getInt(input);
            sequenceNumber = IOHelper.getInt(input);
            messageCreationDate = IOHelper.getLong(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return new ControlPacket(type, senderID, messageCreator, sequenceNumber, messageCreationDate);
    }

    /**
     * Packs a control packet into an array of bytes for sending over UDP
     * @return A byte[] for sending over the wire
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        IOHelper.writeInt(this.type.ordinal(), byteStream);
        IOHelper.writeInt(this.senderID, byteStream);
        IOHelper.writeInt(this.messageCreator, byteStream);
        IOHelper.writeInt(this.sequenceNumber, byteStream);
        IOHelper.writeLong(this.messageCreationDate, byteStream);

        return byteStream.toByteArray();
    }

    /**
     * Type of the control packet
     */
    public Type type;

    /**
     * Identifier for the sender of the control packet
     */
    public int senderID;

    /**
     * Identifier for the sender of the message in question.
     * If senderID = 0 and messageCreator = 1 in a HAVE message,
     * both clients 0 and 1 have the message to distribute, but the message "sender" is client 1
     * For KEEPALIVE, senderID == messageCreator
     */
    public int messageCreator;

    /**
     * Sequence number for message in question.
     * For KEEPALIVE, this is the sequence number of the last packet available from this client.
     */
    public int sequenceNumber;

    /** 
     * Message creation date, in Milliseconds
     * Useful to know which one should be requested next, because they should be displayed in order.
     */
    public long messageCreationDate;

    // possibly keep track of packet creation date?

    /**
     * Make a control packet for sending
     * @param type The type of control packet, like HAVE, UNCHOKE, KEEPALIVE, etc.
     * @param senderID The UUID for the client which is sending the packet
     * @param messageCreator The UUID for the client who made the message in question.
     * @param sequenceNumber The sequence number of the packet in question
     * @param messageCreationDate The Date when the message was created
     */
    public ControlPacket(Type type, int senderID, int messageCreator, int sequenceNumber, long messageCreationDate) {
        this.type = type;
        this.senderID = senderID;
        this.messageCreator = messageCreator;
        this.sequenceNumber = sequenceNumber;
        this.messageCreationDate = messageCreationDate;
    }

    /**
     * Helper method for making a control packet from a message.
     */
    public static ControlPacket packetForMessage(Message message, Type type, int sender) {
        return new ControlPacket(type, sender, message.senderID, message.sequenceNumber, message.date);
    }
}
