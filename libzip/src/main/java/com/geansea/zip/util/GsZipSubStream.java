package com.geansea.zip.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class GsZipSubStream extends InputStream {
    private final RandomAccessFile file;
    private long offset;
    private long endOffset;

    public GsZipSubStream(@NonNull RandomAccessFile file,
                          long offset,
                          long endOffset) throws IOException {
        GsZipUtil.check(0 <= offset, "Error offset");
        GsZipUtil.check(offset <= endOffset, "Error length");
        GsZipUtil.check(endOffset <= file.length(), "Error end offset");
        this.file = file;
        this.offset = offset;
        this.endOffset = endOffset;
    }

    public GsZipSubStream(RandomAccessFile file, long offset) throws IOException {
        this(file, offset, file.length());
    }

    public void resetSize(long size) throws IOException {
        GsZipUtil.check(size >= 0, "Error size");
        endOffset = offset + size;
        GsZipUtil.check(endOffset <= file.length(), "");
    }

    @Override
    public int available() {
        return (offset < endOffset ? 1 : 0);
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int count = read(buffer, 0, 1);
        return (count > 0 ? buffer[0] : -1);
    }

    @Override
    public int read(byte @NonNull[] buffer, int byteOffset, int byteCount) throws IOException {
        synchronized (file) {
            byteCount = Math.min(byteCount, (int) (endOffset - offset));
            file.seek(offset);
            int count = file.read(buffer, byteOffset, byteCount);
            if (count > 0) {
                offset += count;
                return count;
            } else {
                return -1;
            }
        }
    }

    @Override
    public long skip(long byteCount) {
        byteCount = Math.min(byteCount, endOffset - offset);
        offset += byteCount;
        return byteCount;
    }
}
