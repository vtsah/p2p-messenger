/**
 * This class just has static methods helpful to send and receive different types of data through streams.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.math.*;

public class IOHelper {

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
     * Helper function for writing a long to byte array, in an architecture-independent way.
     * @param number Long to write to steam
     * @param byteStream Byte array output stream for writing exactly 8 bytes of long.
     */
    public static void writeLong(long number, ByteArrayOutputStream byteStream) {
        // same pack ideas as used in Transport.java, part of Fishnet.
        byte[] byteArray = (BigInteger.valueOf(number)).toByteArray();
        for (int i = 0; i < 8 - byteArray.length; i++) {
            byteStream.write(0);
        }
        byteStream.write(byteArray, 0, Math.min(byteArray.length, 8));
    }

    /**
     * Helper function for writing a byte array to a byte stream, preceded by the length of the byte array as an int.
     * @param data The byte array to append to stream.
     * @param byteStream The stream to write to.
     */
    public static void writeByteArray(byte[] data, ByteArrayOutputStream byteStream) throws IOException {
        writeInt(data.length, byteStream);
        byteStream.write(data);
    }

    /**
     * Helper function for writing a string to byte array. Appends 4-byte count followed by String in ASCII.
     * @param string The string to append to byte array.
     * @param byteStream Byte array output stream for writing
     */
    public static void writeString(String string, ByteArrayOutputStream byteStream) throws IOException {
        writeByteArray(string.getBytes(StandardCharsets.US_ASCII), byteStream);
    }

    /**
     * Helper function for getting 4 bytes from input as integer; architecture-independent.
     * @param input the input stream
     * @return integer representation of first four bytes.
     */
    public static int getInt(BufferedInputStream input) throws IOException {
        return (new BigInteger(IOHelper.getBytes(input, 4))).intValue();
    }

    /**
     * Helper function for getting 8 bytes from input as long; architecture-independent.
     * @param input the input stream
     * @return Long representation of first eight bytes.
     */
    public static long getLong(BufferedInputStream input) throws IOException {
        return (new BigInteger(IOHelper.getBytes(input, 8))).longValue();
    }

    /**
     * Gets the length of byte array as an integer, followed by the integer.
     * @param input the input stream.
     * @return byte array of length n, after reading n from input.
     */
    public static byte[] getByteArray(BufferedInputStream input) throws IOException {
        int length = IOHelper.getInt(input);
        return IOHelper.getBytes(input, length);
    }

    /**
     * Gets the length of a string as an integer, followed by the string.
     * @param input the input stream
     * @return ASCII string of length n, after reading int n from input.
     */
    public static String getString(BufferedInputStream input) throws IOException {
        return new String(IOHelper.getByteArray(input), StandardCharsets.US_ASCII);
    }

    /**
     * Blocking function to get byte array from input stream (blocking code)
     *
     * @param input the input stream to read from.
     * @param byteCount the number of bytes to read.
     *
     * @return byteCount bytes. Throws IOException if hits EOF first.
     */
    public static byte[] getBytes(BufferedInputStream input, int byteCount) throws IOException {
        byte[] bytes = new byte[byteCount];
        int character = 0;
        int i = 0;
        while (i != byteCount && (character = input.read()) != -1) {
            bytes[i++] = (byte)character;
        }
        if (i < byteCount) {
            throw new IOException();
        }
        return bytes;
    }
}
