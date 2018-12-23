package com.geansea.zip.util;

import com.google.common.base.Preconditions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import javax.annotation.Nonnegative;

public class GsZipCentralDirEnd {
    public static final int MAGIC = 0x06054b50; // "PK\x05\x06"
    public static final int BASE_SIZE = 0x16;

    private int sign;
    private short diskNum;
    private short startDiskNum;
    private short diskEntryNum;
    private short entryNum;
    private int dirSize;
    private int dirOffset;
    private short commentLen;
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
        comment = new byte[commentLen];
    }

    private void checkValid() throws IllegalStateException {
        Preconditions.checkState(sign == MAGIC, "Error sign");
        Preconditions.checkState(diskNum == 0, "Disk number should be 0");
        Preconditions.checkState(startDiskNum == 0, "Start Disk number should be 0");
        Preconditions.checkState(diskEntryNum == entryNum, "Entry number not match");
        Preconditions.checkState(entryNum >= 0, "Error entry number");
    }

    private void checkValidForWrite() throws IllegalStateException {
        checkValid();
    }

    public int getEntryCount() {
        return entryNum;
    }

    public void setEntryCount(@Nonnegative int count) {
        entryNum = (short) count;
        diskEntryNum = entryNum;
    }

    public int getDirOffset() {
        return dirOffset;
    }

    public int getDirSize() {
        return dirSize;
    }

    public void setDirRange(@Nonnegative int offset, @Nonnegative int size) {
        dirOffset = offset;
        dirSize = size;
    }

    public @NonNull String getComment(@NonNull Charset charset) {
        return new String(comment, charset);
    }

    public void setComment(@NonNull String value, @NonNull Charset charset) {
        comment = value.getBytes(charset);
        commentLen = (short) comment.length;
    }

    public void readFrom(byte @NonNull [] bytes) throws IllegalStateException {
        Preconditions.checkState(bytes.length >= byteSize(), "Not enough length");

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        sign = byteBuffer.getInt();
        diskNum = byteBuffer.getShort();
        startDiskNum = byteBuffer.getShort();
        diskEntryNum = byteBuffer.getShort();
        entryNum = byteBuffer.getShort();
        dirSize = byteBuffer.getInt();
        dirOffset = byteBuffer.getInt();
        commentLen = byteBuffer.getShort();
        Preconditions.checkState(byteBuffer.position() == BASE_SIZE, "Error size");
        Preconditions.checkState(bytes.length >= byteSize(), "Not enough length");

        if (commentLen > 0) {
            comment = new byte[commentLen];
            byteBuffer.get(comment);
        }
        checkValid();
    }

    public int byteSize() {
        return BASE_SIZE + commentLen;
    }

    public void writeTo(byte @NonNull [] bytes) throws IllegalStateException {
        checkValidForWrite();
        Preconditions.checkState(bytes.length >= byteSize(), "Not enough length");

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(sign);
        byteBuffer.putShort(diskNum);
        byteBuffer.putShort(startDiskNum);
        byteBuffer.putShort(diskEntryNum);
        byteBuffer.putShort(entryNum);
        byteBuffer.putInt(dirSize);
        byteBuffer.putInt(dirOffset);
        byteBuffer.putShort(commentLen);
        Preconditions.checkState(byteBuffer.position() == BASE_SIZE, "Error size");

        if (commentLen > 0) {
            byteBuffer.put(comment);
        }
    }
}
