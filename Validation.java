/**
 * Computes a hash of a message in order to validate the UDP transmission of the message.
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;

public class Validation {

    // uses CRC described: https://en.wikipedia.org/wiki/Cyclic_redundancy_check
    private static final long CRC_POLYNOMIAL = 0x04C11DB7;
    private static final int CRC_N = 32;

    // & with this to find leading bit of a CRC_N-bit number.
    private static final long LEAD_BIT_MASK = (((long)1) << (CRC_N-1));
    // & with this to find leading bit of an unsigned byte
    private static final short LEAD_BYTE_BIT_MASK = (((short)1) << (8 - 1));

    private static long appendBitToCRC(long crc, boolean bit) {
        long firstBit = LEAD_BIT_MASK & crc;
        crc ^= firstBit; // clear top bit
        crc <<= 1; // move over
        crc += (bit ? 1 : 0);
        if (firstBit != 0) {
            crc ^= CRC_POLYNOMIAL;
        }
        return crc;
    }

    private static long padCRC(long crc) {
        for (int i = 0; i < CRC_N; i++) {
            crc = appendBitToCRC(crc, false);
        }
        return crc;
    }

    /**
     * Appends an byte to a CRC.
     */
    private static long appendByteToCRC(long crc, byte character) {
        short unsignedByte = (short)(0x000000FF & ((int)character));
        for (int i = 0; i < 8; i++) {
            boolean bit = (LEAD_BYTE_BIT_MASK & unsignedByte) != 0;
            crc = appendBitToCRC(crc, bit);
            unsignedByte <<= 1;
        }
        return crc;
    }

    /**
     * Gets the bytes for a message that should be hashed
     */
    private static byte[] messageDataAndMetadata(Message message) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try {
            IOHelper.writeByteArray(message.data, byteStream);
            IOHelper.writeInt(message.senderID, byteStream);
            IOHelper.writeInt(message.blockIndex, byteStream);
            IOHelper.writeInt(message.sequenceNumber, byteStream);
            IOHelper.writeLong(message.date, byteStream);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return byteStream.toByteArray();
    }

    public static long dataCRC(byte[] data) {
        long crc = 0;
        for (int i = 0; i < data.length; i++) {
            crc = appendByteToCRC(crc, data[i]);
        }
        crc = padCRC(crc);
        return crc;
    }

    /**
     * Computes the cyclic redundancy check of a message, including message's data and metadata.
     * @param message The message to compute the hash of.
     * @return A hash of the message.
     */
    public static long computeCRC(Message message) {
        return dataCRC(messageDataAndMetadata(message));
    }
}
