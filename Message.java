/**
 * Encapsulates all data associated with a single unit of data.
 * Typically this would be a single message, but could also be a part of a (very long) message / attachment.
 * For now, only consider small text messages.
 */

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
}
