package com.geansea.zip;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.zip.CRC32;

final class EntryHeader {
    private static final int CENTRAL_MAGIC = 0x02014b50; // "PK\x01\x02"
    private static final int LOCAL_MAGIC = 0x04034b50; // "PK\x03\x04"

    private static final int CENTRAL_HEADER_SIZE = 0x2E;
    private static final int LOCAL_HEADER_SIZE = 0x1E;

    private static final short BITFLAG_ENCRYPTED = 0x0001;
    private static final short BITFLAG_DATA_DESCRIPTOR = 0x0008;
    private static final short BITFLAG_STRONG_ENCRYPTION = 0x0040;
    private static final short BITFLAG_LANGUAGE_UTF8 = 0x0800;

    private static final short BITFLAG_ENCRYPT_MASK = BITFLAG_ENCRYPTED | BITFLAG_STRONG_ENCRYPTION;

    static final short ENCRYPT_NONE = 0x00;
    static final short ENCRYPT_PKWARE = BITFLAG_ENCRYPTED;

    static final short COMPRESS_STORED = 0x00;
    static final short COMPRESS_FLATE = 0x08;

    private static final short UNICODE_PATH_EXTRA_FIELD_ID = 0x7075;

    private int     sign;          // (cl)
    private short   versionMadeBy; // (c)
    private short   versionNeeded; // (cl)
    private short   bitFlags;      // (cl)
    private short   compMethod;    // (cl)
    private short   lastModTime;   // (cl)
    private short   lastModDate;   // (cl)
    private int     CRC;           // (cl) CRC-32
    private int     compSize;      // (cl)
    private int     uncompSize;    // (cl)
    private short   fileNameLen;   // (cl)
    private short   extraFieldLen; // (cl)
    private short   commentLen;    // (c)
    private short   diskNumber;    // (c)
    private short   intAttrib;     // (c)
    private int     extAttrib;     // (c)
    private int     localOffset;   // (c)
    private byte[] fileName;
    private byte[] extraField;
    private byte[] comment;

    EntryHeader() {
        sign = CENTRAL_MAGIC;
        versionMadeBy = 0x0014;
        versionNeeded = 0x0014;
        bitFlags = BITFLAG_LANGUAGE_UTF8;
        compMethod = COMPRESS_STORED;
        lastModTime = 0;
        lastModDate = 0;
        CRC = 0;
        compSize = 0;
        uncompSize = 0;
        fileNameLen = 0;
        extraFieldLen = 0;
        commentLen = 0;
        diskNumber = 0;
        intAttrib = 0;
        extAttrib = 0;
        localOffset = 0;
        fileName = new byte[fileNameLen];
        extraField = new byte[extraFieldLen];
        comment = new byte[commentLen];
    }

    private void checkValid(boolean central) throws GsZipException {
        GsZipUtil.check(sign == (central ? CENTRAL_MAGIC : LOCAL_MAGIC), "Error sign");
        GsZipUtil.check(fileNameLen > 0, "Empty file name");
    }

    private void checkValidForWrite(boolean central) throws GsZipException {
        checkValid(central);
        GsZipUtil.check((bitFlags & BITFLAG_LANGUAGE_UTF8) != 0, "Error encoding for file name");
        GsZipUtil.check(compMethod == COMPRESS_STORED || compMethod == COMPRESS_FLATE, "Error compress method");
    }

    boolean matchLocal(@NonNull EntryHeader header) {
        boolean match = (compMethod == header.compMethod
                && (bitFlags & BITFLAG_ENCRYPT_MASK) == (header.bitFlags & BITFLAG_ENCRYPT_MASK)
                && fileNameLen == header.fileNameLen
                && Arrays.equals(fileName, header.fileName));
        if (match && 0 == (header.bitFlags & BITFLAG_DATA_DESCRIPTOR)) {
            match = (CRC == header.CRC
                    && compSize == header.compSize
                    && uncompSize == header.uncompSize);
        }
        return match;
    }

    void setSign(boolean central) {
        sign = (central ? CENTRAL_MAGIC : LOCAL_MAGIC);
    }

    int getEncMethod() {
        return bitFlags & BITFLAG_ENCRYPT_MASK;
    }

    void setEncMethod(int method) {
        bitFlags |= (short) method;
    }

    int getCompMethod() {
        return compMethod;
    }

    void setCompMethod(int method) {
        compMethod = (short) method;
    }

    @NonNull Date getLastModifiedTime() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(1980 + ((lastModDate >> 9) & 0x7f),
                ((lastModDate >> 5) & 0xf) - 1,
                lastModDate & 0x1f,
                (lastModTime >> 11) & 0x1f,
                (lastModTime >> 5) & 0x3f,
                (lastModTime & 0x1f) << 1);
        return cal.getTime();
    }

    void setLastModifiedTime(@NonNull Date time) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(time);
        int year = cal.get(Calendar.YEAR);
        if (year < 1980) {
            lastModDate = 0x21;
            lastModTime = 0;
        } else {
            lastModDate = (short) (((cal.get(Calendar.YEAR) - 1980) << 9)
                    | ((cal.get(Calendar.MONTH) + 1) << 5)
                    | cal.get(Calendar.DATE));
            lastModTime = (short) ((cal.get(Calendar.HOUR_OF_DAY) << 11)
                    | (cal.get(Calendar.MINUTE) << 5)
                    | (cal.get(Calendar.SECOND) >> 1));
        }
    }

    int getCRC() {
        return CRC;
    }

    void setCRC(int crc) {
        CRC = crc;
    }

    int getCompSize() {
        return compSize;
    }

    void setCompSize(int size) {
        compSize = size;
    }

    int getUncompSize() {
        return uncompSize;
    }

    void setUncompSize(int size) {
        uncompSize = size;
    }

    byte getTimeCheck() {
        return (byte) (lastModTime >>> 8);
    }

    byte getCrcCheck() {
        return (byte) (CRC >>> 24);
    }

    @NonNull String getFileName(@NonNull Charset charset) {
        if ((bitFlags & BITFLAG_LANGUAGE_UTF8) != 0) {
            return new String(fileName, StandardCharsets.UTF_8);
        }
        // Extra Field
        if (extraFieldLen > 0) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(extraField).order(ByteOrder.LITTLE_ENDIAN);
            while (byteBuffer.remaining() >= 4) {
                short headerId = byteBuffer.getShort();
                short dataSize = byteBuffer.getShort();
                if (headerId == UNICODE_PATH_EXTRA_FIELD_ID && dataSize > 5) {
                    byte version = byteBuffer.get();
                    int fileNameCRC = byteBuffer.getInt();
                    byte[] data = new byte[dataSize - 5];
                    byteBuffer.get(data);

                    CRC32 crc32 = new CRC32();
                    crc32.update(fileName);
                    if (version == 1 && fileNameCRC == (int) crc32.getValue()) {
                        return new String(data, StandardCharsets.UTF_8);
                    }
                } else {
                    byte[] data = new byte[dataSize];
                    byteBuffer.get(data);
                }
            }
        }
        // Force convert
        return new String(fileName, charset);
    }

    void setFileName(@NonNull String name) {
        fileName = name.getBytes(StandardCharsets.UTF_8);
        fileNameLen = (short) fileName.length;
    }

    int getLocalOffset() {
        return localOffset;
    }

    void setLocalOffset(int offset) {
        localOffset = offset;
    }

    void readFrom(@NonNull InputStream stream, boolean central) throws IOException, GsZipException {
        int headerSize = central ? CENTRAL_HEADER_SIZE : LOCAL_HEADER_SIZE;
        byte[] bytes = new byte[headerSize];
        GsZipUtil.check(stream.read(bytes) == bytes.length, "Read fail");

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        sign = byteBuffer.getInt();
        versionMadeBy = central ? byteBuffer.getShort() : 0;
        versionNeeded = byteBuffer.getShort();
        bitFlags = byteBuffer.getShort();
        compMethod = byteBuffer.getShort();
        lastModTime = byteBuffer.getShort();
        lastModDate = byteBuffer.getShort();
        CRC = byteBuffer.getInt();
        compSize = byteBuffer.getInt();
        uncompSize = byteBuffer.getInt();
        fileNameLen = byteBuffer.getShort();
        extraFieldLen = byteBuffer.getShort();
        commentLen = central ? byteBuffer.getShort() : 0;
        diskNumber = central ? byteBuffer.getShort() : 0;
        intAttrib = central ? byteBuffer.getShort() : 0;
        extAttrib = central ? byteBuffer.getInt() : 0;
        localOffset = central ? byteBuffer.getInt() : 0;
        GsZipUtil.check(byteBuffer.remaining() == 0, "Error size");

        fileName = new byte[fileNameLen];
        if (fileNameLen > 0) {
            GsZipUtil.check(stream.read(fileName) == fileNameLen, "Read fail");
        }
        extraField = new byte[extraFieldLen];
        if (extraFieldLen > 0) {
            GsZipUtil.check(stream.read(extraField) == extraFieldLen, "Read fail");
        }
        comment = new byte[commentLen];
        if (commentLen > 0) {
            GsZipUtil.check(stream.read(comment) == commentLen, "Read fail");
        }
        checkValid(central);
    }

    int byteSize(boolean central) {
        if (central) {
            return CENTRAL_HEADER_SIZE + fileNameLen + extraFieldLen + commentLen;
        } else {
            return LOCAL_HEADER_SIZE + fileNameLen + extraFieldLen;
        }
    }

    void writeTo(@NonNull OutputStream stream, boolean central) throws IOException, GsZipException {
        checkValidForWrite(central);
        int headerSize = central ? CENTRAL_HEADER_SIZE : LOCAL_HEADER_SIZE;
        byte[] bytes = new byte[headerSize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(sign);
        if (central) {
            byteBuffer.putShort(versionMadeBy);
        }
        byteBuffer.putShort(versionNeeded);
        byteBuffer.putShort(bitFlags);
        byteBuffer.putShort(compMethod);
        byteBuffer.putShort(lastModTime);
        byteBuffer.putShort(lastModDate);
        byteBuffer.putInt(CRC);
        byteBuffer.putInt(compSize);
        byteBuffer.putInt(uncompSize);
        byteBuffer.putShort(fileNameLen);
        byteBuffer.putShort(extraFieldLen);
        if (central) {
            byteBuffer.putShort(commentLen);
            byteBuffer.putShort(diskNumber);
            byteBuffer.putShort(intAttrib);
            byteBuffer.putInt(extAttrib);
            byteBuffer.putInt(localOffset);
        }
        GsZipUtil.check(byteBuffer.remaining() == 0, "Error size");
        stream.write(bytes);

        if (fileNameLen > 0) {
            stream.write(fileName);
        }
        if (extraFieldLen > 0) {
            stream.write(extraField);
        }
        if (commentLen > 0) {
            stream.write(comment);
        }
    }
}
