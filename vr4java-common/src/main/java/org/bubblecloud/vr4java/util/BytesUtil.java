package org.bubblecloud.vr4java.util;

import java.io.UnsupportedEncodingException;

/**
 * Class for converting primitives to bytes and back.
 * 
 * @author Tommi Laukkanen
 */
public final class BytesUtil {

    /**
     * Disable construction.
     */
    private BytesUtil() {
        throw new UnsupportedOperationException();
    }

    /** 0 bits for shift. */
    private static final int BITS_0 = 0;
    /** 8 bits for shift. */
    private static final int BITS_8 = 8;
    /** 16 bits for shift. */
    private static final int BITS_16 = 16;
    /** 24 bits for shift. */
    private static final int BITS_24 = 24;
    /** 32 bits for shift. */
    private static final int BITS_32 = 32;
    /** 40 bits for shift. */
    private static final int BITS_40 = 40;
    /** 48 bits for shift. */
    private static final int BITS_48 = 48;
    /** 56 bits for shift. */
    private static final int BITS_56 = 56;
    /** Maximum value of byte. */
    private static final int BYTE_MAX = 0xFF;
    /** UTF8 charset. */
    private static final String UTF8 = "UTF-8";

    /**
     * Write long value to buffer starting at given index.
     * 
     * @param value the value
     * @param buffer the buffer to write to
     * @param startIndex the starting index in buffer
     */
    public static void writeLong(final long value, final byte[] buffer, final int startIndex) {
        int currentIndex = startIndex;
        buffer[currentIndex++] = (byte) (value >>> BITS_56);
        buffer[currentIndex++] = (byte) (value >>> BITS_48);
        buffer[currentIndex++] = (byte) (value >>> BITS_40);
        buffer[currentIndex++] = (byte) (value >>> BITS_32);
        buffer[currentIndex++] = (byte) (value >>> BITS_24);
        buffer[currentIndex++] = (byte) (value >>> BITS_16);
        buffer[currentIndex++] = (byte) (value >>> BITS_8);
        buffer[currentIndex++] = (byte) (value >>> BITS_0);
    }

    /**
     * Read long value from buffer starting at given index.
     * 
     * @param buffer the buffer to read from
     * @param startIndex the starting index in buffer
     * @return the read value
     */
    public static long readLong(final byte[] buffer, final int startIndex) {
        int currentIndex = startIndex;
        return ((long) buffer[currentIndex++] << BITS_56) + ((long) (buffer[currentIndex++] & BYTE_MAX) << BITS_48)
                + ((long) (buffer[currentIndex++] & BYTE_MAX) << BITS_40)
                + ((long) (buffer[currentIndex++] & BYTE_MAX) << BITS_32)
                + ((long) (buffer[currentIndex++] & BYTE_MAX) << BITS_24)
                + ((buffer[currentIndex++] & BYTE_MAX) << BITS_16) + ((buffer[currentIndex++] & BYTE_MAX) << BITS_8)
                + ((buffer[currentIndex++] & BYTE_MAX) << BITS_0);
    }

    /**
     * Write integer value to buffer starting at given index.
     * 
     * @param value the value
     * @param buffer the buffer to write to
     * @param startIndex the starting index in buffer
     */
    public static void writeInteger(final int value, final byte[] buffer, final int startIndex) {
        int currentIndex = startIndex;
        buffer[currentIndex++] = (byte) (value >>> BITS_24);
        buffer[currentIndex++] = (byte) (value >>> BITS_16);
        buffer[currentIndex++] = (byte) (value >>> BITS_8);
        buffer[currentIndex++] = (byte) (value >>> BITS_0);
    }

    /**
     * Read integer value from buffer starting at given index.
     * 
     * @param buffer the buffer to read from
     * @param startIndex the starting index in buffer
     * @return the read value
     */
    public static int readInteger(final byte[] buffer, final int startIndex) {
        int currentIndex = startIndex;
        return (buffer[currentIndex++] << BITS_24) + ((buffer[currentIndex++] & BYTE_MAX) << BITS_16)
                + ((buffer[currentIndex++] & BYTE_MAX) << BITS_8) + ((buffer[currentIndex++] & BYTE_MAX) << BITS_0);
    }

    /**
     * Write short value to buffer starting at given index.
     * 
     * @param value the value
     * @param buffer the buffer to write to
     * @param startIndex the starting index in buffer
     */
    public static void writeShort(final short value, final byte[] buffer, final int startIndex) {
        int currentIndex = startIndex;
        buffer[currentIndex++] = (byte) (value >>> BITS_8);
        buffer[currentIndex++] = (byte) (value >>> BITS_0);
    }

    /**
     * Read short value from buffer starting at given index.
     * 
     * @param buffer the buffer to read from
     * @param startIndex the starting index in buffer
     * @return the read value
     */
    public static short readShort(final byte[] buffer, final int startIndex) {
        int currentIndex = startIndex;
        return (short) ((buffer[currentIndex++] << BITS_8) + ((buffer[currentIndex++] & BYTE_MAX) << BITS_0));
    }

    /**
     * Write String value to buffer starting at given index and with given max
     * length.
     * 
     * @param value the value
     * @param buffer the buffer to write to
     * @param startIndex the starting index in buffer
     * @param length length of bytes reserved in buffer for this string.
     */
    public static void writeString(final String value, final byte[] buffer, final int startIndex, final int length) {
        byte[] bytes = null;
        try {
            bytes = value.getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            new RuntimeException(e);
        }
        if (bytes.length > BYTE_MAX || bytes.length > length - 1) {
            throw new RuntimeException("String value too large.");
        }
        buffer[startIndex] = (byte) bytes.length;
        for (int i = 0; i < length - 1; i++) {
            if (i < bytes.length) {
                buffer[startIndex + i + 1] = bytes[i];
            } else {
                buffer[startIndex + i + 1] = 0;
            }
        }
    }

    /**
     * Read String value from buffer starting at given index.
     * 
     * @param buffer the buffer to read from
     * @param startIndex the starting index in buffer
     * @param length length of bytes reserved in buffer for this string.
     * @return the read value
     */
    public static String readString(final byte[] buffer, final int startIndex, final int length) {
        final byte bytesLength = buffer[startIndex];
        final byte[] bytes = new byte[bytesLength];
        for (int i = 0; i < bytesLength; i++) {
            bytes[i] = buffer[startIndex + i + 1];
        }
        try {
            return new String(bytes, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write byte array to buffer starting at given index and with given max
     * length.
     * 
     * @param bytes the byte array to write
     * @param buffer the buffer to write to
     * @param startIndex the starting index in buffer
     * @param length length of bytes reserved in buffer for this string.
     */
    public static void writeBytes(final byte[] bytes, final byte[] buffer, final int startIndex, final int length) {
        for (int i = 0; i < length; i++) {
            if (i < bytes.length) {
                buffer[startIndex + i] = bytes[i];
            } else {
                buffer[startIndex + i] = 0;
            }
        }
    }

    /**
     * Read byte array from buffer starting at given index.
     * 
     * @param buffer the buffer to read from
     * @param startIndex the starting index in buffer
     * @param length length of bytes reserved in buffer for this string.
     * @return the read bytes
     */
    public static byte[] readBytes(final byte[] buffer, final int startIndex, final int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = buffer[startIndex + i];
        }
        return bytes;
    }


    /**
     * Read short value from two bytes.
     * 
     * @param byte0 byte zero.
     * @param byte1 byte one.
     * @return the read value
     */
    public static short bytesToShort(final byte byte0, final byte byte1) {
        return (short) ((byte0 << BITS_8) + ((byte1 & BYTE_MAX) << BITS_0));
    }
    
    /**
     * Converts byte to integer.
     * @param b byte to convert.
     * @return integer value.
     */
    public static int byteToInteger(final byte b) {
        return (int) b & BYTE_MAX;
    }
    
    
}