import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

/**
 * Represents a user, with relevant properties associated.
 * The first four bytes of user's representation is always the ID.
 */
public class User {
    /**
     * Public username
     */
    public String username = null;

    /**
     * IP address, as determined by the received packet.
     */
    public InetAddress address = null;

    /**
     * User's port for receiving control messages.
     */
    public int port;

    /**
     * User's port for sending data (TCP connections with Server Socket)
     */
    public int dataPort;

    /**
     * User's unique identifier in the group
     */
    public int userID;

    /**
     * @param username User's public username
     * @param address User's address from packet
     * @param port User's port
     */
    public User(String username, InetAddress address, int port, int dataPort, int userID) {
        this.username = username;
        this.address = address;
        this.port = port;
        this.dataPort = dataPort;
        this.userID = userID;
    }

    /**
     * Convert a User object into a byte array for transport over the wire.
     * Architecture-independent.
     * @return A byte[] for transport over the wire.
     */
    public byte[] pack() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            IOHelper.writeInt(this.userID, byteStream);

            IOHelper.writeByteArray(this.address.getAddress(), byteStream);

            IOHelper.writeInt(this.port, byteStream);
            IOHelper.writeInt(this.dataPort, byteStream);

            IOHelper.writeString(this.username, byteStream);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return byteStream.toByteArray();
    }

    /**
     * Create a user object from a stream of data
     */
    public static User unpack(BufferedInputStream input) {
        int userID = 0;
        try {
            userID = IOHelper.getInt(input);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return User.unpackWithID(userID, input);
    }

    /**
     * Unpack after already extracting ID
     */
    public static User unpackWithID(int userID, BufferedInputStream input) {
        InetAddress ipAddress = null;
        int dataPort = 0;
        int port = 0;
        String username = null;
        try {
            ipAddress = InetAddress.getByAddress(IOHelper.getByteArray(input));

            port = IOHelper.getInt(input);
            dataPort = IOHelper.getInt(input);

            username = IOHelper.getString(input);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return new User(username, ipAddress, port, dataPort, userID);
    }

}
