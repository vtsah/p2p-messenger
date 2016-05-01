import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

/**
 * Represents a user, with relevant properties associated
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
            byte[] addressBytes = this.address.getAddress();
            IOHelper.writeInt(addressBytes.length, byteStream);
            byteStream.write(addressBytes);

            IOHelper.writeInt(this.port, byteStream);
            IOHelper.writeInt(this.dataPort, byteStream);

            IOHelper.writeInt(this.userID, byteStream);

            IOHelper.writeString(this.username, byteStream);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return byteStream.toByteArray();
    }

    public static User unpack(BufferedInputStream input) {
        InetAddress ipAddress = null;
        int dataPort = 0;
        int port = 0;
        int userID = 0;
        String username = null;
        try {
            // IP address is n bytes
            int ipLength = IOHelper.getInt(input);
            byte[] ip = IOHelper.getBytes(input, ipLength);
            ipAddress = InetAddress.getByAddress(ip);

            port = IOHelper.getInt(input);
            dataPort = IOHelper.getInt(input);

            userID = IOHelper.getInt(input);

            username = IOHelper.getString(input);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return new User(username, ipAddress, port, dataPort, userID);
    }

}
