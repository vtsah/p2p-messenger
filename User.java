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
     * User's port
     */
    public int port;

    /**
     * @param username User's public username
     * @param address User's address from packet
     * @param port User's port
     */
    public User(String username, InetAddress address, int port) {
        this.username = username;
        this.address = address;
        this.port = port;
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
            User.writeInt(addressBytes.length, byteStream);
            byteStream.write(addressBytes);

            User.writeInt(this.port, byteStream);

            User.writeString(this.username, byteStream);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return byteStream.toByteArray();
    }

    /**
     * Helper function for writing an int to byte array, in an architecture-independent way.
     * @param number Integer to write to steam
     * @param byteStream Byte array output stream for writing exactly 4 bytes of integer.
     */
    public static void writeInt(int number, ByteArrayOutputStream byteStream) {
        // same pack ideas as used in Transport.java, part of Fishnet.
        byte[] byteArray = (BigInteger.valueOf(number)).toByteArray();
        for (int i = 0; i < 4 - byteArray.length; i++) {
            byteStream.write(0);
        }
        byteStream.write(byteArray, 0, Math.min(byteArray.length, 4));
    }

    /**
     * Helper function for writing a string to byte array. Appends 4-byte count followed by String in ASCII.
     * @param string The string to append to byte array.
     * @param byteStream Byte array output stream for writing
     */
    public static void writeString(String string, ByteArrayOutputStream byteStream) throws IOException {
        byte[] stringBytes = string.getBytes(StandardCharsets.US_ASCII);
        writeInt(stringBytes.length, byteStream);
        byteStream.write(stringBytes);
    }

    /**
     * Helper function for getting 4 bytes from input as integer; architecture-independent.
     * @param input the input stream
     * @return integer representation of first four bytes.
     */
    public static int getInt(BufferedInputStream input) throws Exception {
        return (new BigInteger(User.getBytes(input, 4))).intValue();
    }

    /**
     * Gets the length of a string as an integer, followed by the string.
     * @param input the input stream
     * @return ASCII string of length n, after reading int n from input.
     */
    public static String getString(BufferedInputStream input) throws Exception {
        int length = User.getInt(input);
        return new String(User.getBytes(input, length), StandardCharsets.US_ASCII);
    }

    /**
     * Blocking function to get byte array from input stream (blocking code)
     *
     * @param input the input stream to read from.
     * @param byteCount the number of bytes to read.
     *
     * @return byteCount bytes, 0-padded if reached EOF
     */
    public static byte[] getBytes(BufferedInputStream input, int byteCount) throws Exception {
        byte[] bytes = new byte[byteCount];
        int character = 0;
        int i = 0;
        while ((character = input.read()) != -1) {
            bytes[i++] = (byte)character;
            if (i == byteCount) break;
        }
        while (i < byteCount) {
            bytes[i++] = 0;
        }
        return bytes;
    }


}
