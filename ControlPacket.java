/**
 * Packs and unpacks control packets. All meta-communication between clients is done with control packets; messages are sent separately.
 */

public class ControlPacket {

    // see https://wiki.theory.org/BitTorrentSpecification
    public enum Type {
        HAVE, // send when have just successfully received / downloaded a single message
        CHOKE, // don't make any requests
        UNCHOKE, // allowing a request to be made.
        INTERESTED, // send when you are interested in a certain packet
        KEEPALIVE, // send lots of these messages to make sure
    };

    /**
     * Unpacks a control packet from an array of bytes received through UDP.
     * @param data the array of bytes received in a control packet sent over the wire
     * @return An initialized ControlPacket representing all of the data
     */
    public static ControlPacket unpack(byte[] data) {
        ControlPacket packet = new ControlPacket();

        return null;
    }
}