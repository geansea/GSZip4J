package com.geansea.zip.util;

import com.geansea.zip.GsZipException;
import com.geansea.zip.GsZipUtil;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class GsZipSubStream extends InputStream {
    private final RandomAccessFile file;
    private int offset;
    private int endOffset;

    public GsZipSubStream(@NonNull RandomAccessFile file,
                          @NonNegative int offset,
                          @NonNegative int endOffset) throws IOException, IndexOutOfBoundsException, GsZipException {
        GsZipUtil.check(offset < endOffset, "Error length");
        GsZipUtil.check(endOffset < file.length(), "Error end offset");
        this.file = file;
        this.offset = offset;
        this.endOffset = endOffset;
    }

    public GsZipSubStream(@NonNull RandomAccessFile file, int offset) throws IOException, IndexOutOfBoundsException, GsZipException {
        this(file, offset, (int) file.length());
    }

    public void resetSize(@NonNegative int size) throws GsZipException, IOException {
        endOffset = offset + size;
        GsZipUtil.check(endOffset < file.length(), "Error end offset");
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
    public int read(byte @NonNull [] buffer, int byteOffset, int byteCount) throws IOException {
        synchronized (file) {
            byteCount = Math.min(byteCount, endOffset - offset);
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
