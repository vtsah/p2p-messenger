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

    public enum Type {
        FILE,
        TEXT
    };

    /**
     * Is this message part of a text message or a file? Default to text.
     */
    public Type type = Type.TEXT;

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
     * The number of messages comprising this message's block.
     */
    public int blockSize = 0;

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
    public Message(Type type, byte[] data, int senderID, int blockIndex, int blockSize, int sequenceNumber, long date) {
        this.type = type;
        this.data = data;
        this.senderID = senderID;
        this.blockIndex = blockIndex;
        this.blockSize = blockSize;
        this.sequenceNumber = sequenceNumber;
        this.date = date;
    }

    /**
     * Pack up a message in an array for sending over the wire.
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try {
            IOHelper.writeInt(this.type.ordinal(), byteStream);
            IOHelper.writeByteArray(this.data, byteStream);
            IOHelper.writeInt(this.senderID, byteStream);
            IOHelper.writeInt(this.blockIndex, byteStream);
            IOHelper.writeInt(this.blockSize, byteStream);
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
        int senderID, blockIndex, blockSize, sequenceNumber;
        Type type;
        long date;
        try {
            type = Type.values()[IOHelper.getInt(input)];
            data = IOHelper.getByteArray(input);
            senderID = IOHelper.getInt(input);
            blockIndex = IOHelper.getInt(input);
            blockSize = IOHelper.getInt(input);
            sequenceNumber = IOHelper.getInt(input);
            date = IOHelper.getLong(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return new Message(type, data, senderID, blockIndex, blockSize, sequenceNumber, date);
    }
}
