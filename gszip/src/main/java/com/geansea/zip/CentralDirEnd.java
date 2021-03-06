package com.geansea.zip;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

final class CentralDirEnd {
    static final int MAGIC = 0x06054b50; // "PK\x05\x06"
    static final int BASE_SIZE = 0x16;

    private int sign;
    private short diskNum;
    private short startDiskNum;
    private short diskEntryNum;
    private short entryNum;
    private int dirSize;
    private int dirOffset;
    private short commentLen;
    private byte[] comment;

    CentralDirEnd() {
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

    int getEntryCount() {
        return entryNum & 0xFFFF;
    }

    void setEntryCount(int count) {
        entryNum = (short) count;
        diskEntryNum = entryNum;
    }

    long getDirOffset() {
        return ((long) dirOffset) & 0xFFFFFFFFL;
    }

    long getDirSize() {
        return ((long) dirSize) & 0xFFFFFFFFL;
    }

    void setDirRange(long offset, long size) {
        dirOffset = (int) offset;
        dirSize = (int) size;
    }

    @NonNull
    String getComment(@NonNull Charset charset) {
        return new String(comment, charset);
    }

    void setComment(@NonNull String value, @NonNull Charset charset) {
        comment = value.getBytes(charset);
        commentLen = (short) comment.length;
    }

    void readFrom(@NonNull byte[] bytes) throws GsZipException {
        GsZipUtil.check(bytes.length >= BASE_SIZE, "Not enough length");

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        sign = byteBuffer.getInt();
        GsZipUtil.check(sign == MAGIC, "Error sign");
        diskNum = byteBuffer.getShort();
        GsZipUtil.check(diskNum == 0, "Disk number should be 0");
        startDiskNum = byteBuffer.getShort();
        GsZipUtil.check(startDiskNum == 0, "Start Disk number should be 0");
        diskEntryNum = byteBuffer.getShort();
        entryNum = byteBuffer.getShort();
        GsZipUtil.check(diskEntryNum == entryNum, "Entry number not match");
        dirSize = byteBuffer.getInt();
        dirOffset = byteBuffer.getInt();
        commentLen = byteBuffer.getShort();
        comment = new byte[commentLen];
        GsZipUtil.check(byteBuffer.position() == BASE_SIZE, "Error size");
        GsZipUtil.check(bytes.length >= byteSize(), "Not enough length");
        byteBuffer.get(comment);
    }

    int byteSize() {
        return BASE_SIZE + commentLen & 0xFFFF;
    }

    void writeTo(@NonNull byte[] bytes) throws GsZipException {
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
        byteBuffer.put(comment);
    }
}
