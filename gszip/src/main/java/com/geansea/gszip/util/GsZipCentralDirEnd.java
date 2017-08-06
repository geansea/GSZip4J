package com.geansea.gszip.util;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class GsZipCentralDirEnd {
    public static final int MAGIC = 0x06054b50; // "PK\x05\x06"
    public static final int BASE_SIZE = 0x16;

    private int    sign;
    private short  diskNum;
    private short  startDiskNum;
    private short  diskEntryNum;
    private short  entryNum;
    private int    dirSize;
    private int    dirOffset;
    private short  commentLen;
    private byte[] comment;

    public GsZipCentralDirEnd() {
        sign = MAGIC;
        diskNum = 0;
        startDiskNum = 0;
        diskEntryNum = 0;
        entryNum = 0;
        dirSize = 0;
        dirOffset = 0;
        commentLen = 0;
        comment = null;
    }

    private void checkValid() throws IOException {
        GsZipUtil.check(sign == MAGIC, "Error sign");
        GsZipUtil.check(diskNum == 0, "Disk number should be 0");
        GsZipUtil.check(startDiskNum == 0, "Start Disk number should be 0");
        GsZipUtil.check(diskEntryNum == entryNum, "Entry number not match");
        GsZipUtil.check(entryNum >= 0, "Error entry number");
    }

    private void checkValidForWrite() throws IOException {
        checkValid();
    }

    public int getEntryCount() {
        return entryNum;
    }

    public void setEntryCount(int count) {
        entryNum = (short) count;
        diskEntryNum = entryNum;
    }

    public int getDirOffset() {
        return dirOffset;
    }

    public int getDirSize() {
        return dirSize;
    }

    public void setDirRange(int offset, int size) {
        dirOffset = offset;
        dirSize = size;
    }

    public @NonNull String getComment(@NonNull Charset charset) {
        return ((commentLen > 0) ? new String(comment, charset) : "");
    }

    public void setComment(@NonNull String value, @NonNull Charset charset) {
        commentLen = (short) value.length();
        comment = null;
        if (commentLen > 0) {
            comment = value.getBytes(charset);
        }
    }

    public void readFrom(@NonNull byte[] bytes) throws IOException {
        GsZipUtil.check(bytes.length >= byteSize(), "Not enough length");

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        sign = byteBuffer.getInt();
        diskNum = byteBuffer.getShort();
        startDiskNum = byteBuffer.getShort();
        diskEntryNum = byteBuffer.getShort();
        entryNum = byteBuffer.getShort();
        dirSize = byteBuffer.getInt();
        dirOffset = byteBuffer.getInt();
        commentLen = byteBuffer.getShort();
        GsZipUtil.check(byteBuffer.position() == BASE_SIZE, "Error size");
        GsZipUtil.check(bytes.length >= byteSize(), "Not enough length");

        if (commentLen > 0) {
            comment = new byte[commentLen];
            byteBuffer.get(comment);
        }
        checkValid();
    }

    public int byteSize() {
        return BASE_SIZE + commentLen;
    }

    public void writeTo(@NonNull byte[] bytes) throws IOException {
        checkValidForWrite();
        GsZipUtil.check(bytes.length >= byteSize(), "Not enough length");

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(sign);
        byteBuffer.putShort(diskNum);
        byteBuffer.putShort(startDiskNum);
        byteBuffer.putShort(diskEntryNum);
        byteBuffer.putShort(entryNum);
        byteBuffer.putInt(dirSize);
        byteBuffer.putInt(dirOffset);
        byteBuffer.putShort(commentLen);
        GsZipUtil.check(byteBuffer.position() == BASE_SIZE, "Error size");

        if (commentLen > 0) {
            byteBuffer.put(comment);
        }
    }
}
