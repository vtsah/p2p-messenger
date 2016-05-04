/**
 * Encapsulates all data associated with a single unit of data, a "Piece", which can be sent over the wire all at once.
 * Typically this would be a single message, but could also be a part of a (very long) message / attachment.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Message {

    /**
     * Defines the maximum size of a "Piece", in bytes.
     */
    public static final int MAX_PIECE = 5;

    /**
     * Data sent in message (probably ASCII representation of a string, but maybe part of attachment)
     */
    public byte[] data = null;

    /**
     * Unique Sender identifier.
     */
    public int senderID = 0;

    /**
     * Block index indicates which block this is, as sent by sender. 
     * If a large block (a file's data or large text typed in) is broken into smaller pieces, those pieces are the message
     * and each message gets the same block index.
     */
    public int blockIndex = 0;

    /**
     * Sequence number as sent by sender. So if this is the fifth message the sender wrote, sequenceNumber is 4.
     */
    public int sequenceNumber = 0;

    /**
     * Date message was created
     * Determines order in which messages should be displayed.
     * Time in milliseconds, determined by System.currentTimeMillis()
     */
    public long date = 0;

    /**
     * @param data The content of the message.
     * @param senderID The unique identifier of the sending User.
     * @param sequenceNumber The index of the message as sent by the sender.
     */
    public Message(byte[] data, int senderID, int blockIndex, int sequenceNumber, long date) {
        this.data = data;
        this.senderID = senderID;
        this.blockIndex = blockIndex;
        this.sequenceNumber = sequenceNumber;
        this.date = date;
    }

    /**
     * Pack up a message in an array for sending over the wire.
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try {
            IOHelper.writeByteArray(this.data, byteStream);
            IOHelper.writeInt(this.senderID, byteStream);
            IOHelper.writeInt(this.blockIndex, byteStream);
            IOHelper.writeInt(this.sequenceNumber, byteStream);
            IOHelper.writeLong(this.date, byteStream);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return byteStream.toByteArray();
    }

    /** 
     * Unpack a message from sending over the wire.
     */
    public static Message unpack(BufferedInputStream input) {
        byte[] data = null;
        int senderID, blockIndex, sequenceNumber;
        long date;
        try {
            data = IOHelper.getByteArray(input);
            senderID = IOHelper.getInt(input);
            blockIndex = IOHelper.getInt(input);
            sequenceNumber = IOHelper.getInt(input);
            date = IOHelper.getLong(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return new Message(data, senderID, blockIndex, sequenceNumber, date);
    }
}
