package com.geansea.zip;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Sub-stream for random access file.
 * This is usually the base stream of GsZipInputStream.
 */
final class SubStream extends GsZipInputStream {
    private final RandomAccessFile file;
    private final long start;
    private final long end;
    private long offset;

    SubStream(@NonNull RandomAccessFile file,
              @NonNegative long start,
              @NonNegative long end) throws IOException, GsZipException {
        GsZipUtil.check(start <= end,
                "Start position should br no greater than end position");
        GsZipUtil.check(end <= file.length(),
                "End position should be no greater than file length");
        this.file = file;
        this.start = start;
        this.end = end;
        restart();
    }

    SubStream(@NonNull RandomAccessFile file,
              @NonNegative long start) throws IOException, GsZipException {
        this(file, start, file.length());
    }

    @Override
    public int available() {
        return (offset < end ? 1 : 0);
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int count = read(buffer);
        return (count > 0 ? buffer[0] : -1);
    }

    @Override
    public int read(byte @NonNull [] buffer,
                    @NonNegative int byteOffset,
                    @NonNegative int byteCount) throws IOException {
        if (byteCount == 0) {
            return 0;
        }
        if (offset >= end) {
            return -1;
        }
        int count = Math.min(byteCount, (int) (end - offset));
        synchronized (file) {
            file.seek(offset);
            count = file.read(buffer, byteOffset, count);
        }
        if (count > 0) {
            offset += count;
            return count;
        } else {
            return -1;
        }
    }

    @Override
    public void restart() {
        offset = start;
    }
}
