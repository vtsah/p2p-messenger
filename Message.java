/**
 * Encapsulates all data associated with a single unit of data.
 * Typically this would be a single message, but could also be a part of a (very long) message / attachment.
 * For now, only consider small text messages.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class Message {

    /**
     * Data sent in message (probably ASCII representation of a string, but maybe part of attachment)
     */
    public byte[] data = null;

    /**
     * Unique Sender identifier.
     */
    public int senderID = 0;

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
    public Message(byte[] data, int senderID, int sequenceNumber, long date) {
        this.data = data;
        this.senderID = senderID;
        this.sequenceNumber = sequenceNumber;
        this.date = date;
    }

    /**
     * Pack up a message in an array for sending over the wire.
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try {
            IOHelper.writeInt(this.data.length, byteStream);
            byteStream.write(this.data);
            IOHelper.writeInt(this.senderID, byteStream);
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
        int senderID, sequenceNumber;
        long date;
        try {
            int dataLength = IOHelper.getInt(input);
            data = IOHelper.getBytes(input, dataLength);
            senderID = IOHelper.getInt(input);
            sequenceNumber = IOHelper.getInt(input);
            date = IOHelper.getLong(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return new Message(data, senderID, sequenceNumber, date);
    }
}
