/**
 * Packs and unpacks control packets. All meta-communication between clients is done with control packets; messages are sent separately.
 * Send a packet by .pack()'ing and having a Peer send the data.
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
        CANCEL, // to cancel an unchoke if no longer needed.
    };

    /**
     * Unpacks a control packet from an array of bytes received through UDP.
     * @param data the array of bytes received in a control packet sent over the wire
     * @return An initialized ControlPacket representing all of the data
     */
    public static ControlPacket unpack(byte[] data) {

        Type type;
        int senderID = 0;
        Message message = null;

        try {
            BufferedInputStream input = new BufferedInputStream(new ByteArrayInputStream(data));

            type = Type.values()[IOHelper.getInt(input)];
            senderID = IOHelper.getInt(input);

            if (type == Type.HAVE || type == Type.INTERESTED) {
                long date = 0;
                int blockIndex = 0;
                int messageCreator = IOHelper.getInt(input);
                int sequenceNumber = IOHelper.getInt(input);
                
                if (type == Type.HAVE) {
                    blockIndex = IOHelper.getInt(input);
                    date = IOHelper.getLong(input);
                }
                message = new Message(null, null, messageCreator, blockIndex, 0, sequenceNumber, date);
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return new ControlPacket(type, senderID, message);
    }

    /**
     * Packs a control packet into an array of bytes for sending over UDP
     * @return A byte[] for sending over the wire
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        IOHelper.writeInt(this.type.ordinal(), byteStream);
        IOHelper.writeInt(this.senderID, byteStream);

        if (this.type == Type.HAVE || this.type == Type.INTERESTED) {
            IOHelper.writeInt(this.message.senderID, byteStream);
            IOHelper.writeInt(this.message.sequenceNumber, byteStream);

            if (this.type == Type.HAVE) {
                IOHelper.writeInt(this.message.blockIndex, byteStream);
                IOHelper.writeLong(this.message.date, byteStream);
            }
        }
        
        return byteStream.toByteArray();
    }

    /**
     * Type of the control packet
     */
    public Type type;

    /**
     * The message in question. Depending on the type of packet, not all of the message's properties may be used.
     * For KEEPALIVE, CANCEL, CHOKE, and UNCHOKE, the entire message is ignored; only the sender matters.
     * The `data` and 'blockSize' fields of this message are always ignored.
     * For HAVE, every other field counts, advertising the entire message with all of its metadata.
     * For INTERESTED, `date` and `blockIndex` are ignored.
     */
    public Message message;

    /**
     * Identifier for the sender of the control packet
     */
    public int senderID;

    /**
     * Make a control packet for sending
     * @param type The type of control packet, like HAVE, UNCHOKE, KEEPALIVE, etc.
     * @param senderID The UUID for the client which is sending the packet
     * @param message The Message in question, with properties set as needed.
     */
    public ControlPacket(Type type, int senderID, Message message) {
        this.type = type;
        this.senderID = senderID;
        this.message = message;
    }
}
