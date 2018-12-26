package com.geansea.zip;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class InflaterInputStream extends GsZipInputStream {
    private static final int BUFFER_SIZE = 1024;

    private final GsZipInputStream base;
    private final Inflater inflater;
    private final byte[] inputBuffer;
    private int inputLength;

    InflaterInputStream(@NonNull GsZipInputStream base) throws IOException {
        this.base = base;
        inflater = new Inflater(true);
        inputBuffer = new byte[BUFFER_SIZE];
        restart();
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return inflater.finished() ? 0 : 1;
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        byte[] buffer = new byte[1];
        int count = read(buffer);
        return (count > 0 ? Byte.toUnsignedInt(buffer[0]) : -1);
    }

    @Override
    public int read(byte @NonNull [] b,
                    @NonNegative int off,
                    @NonNegative int len) throws IOException {
        ensureOpen();
        if (len == 0) {
            return 0;
        }
        if (inflater.finished()) {
            return -1;
        }
        try {
            int count = 0;
            while (count < len) {
                int n = inflater.inflate(b, off + count, len - count);
                if (n == 0) {
                    if (inflater.finished()) {
                        break;
                    }
                    if (inflater.needsDictionary()) {
                        count = -1;
                        break;
                    }
                    if (inflater.needsInput()) {
                        fillInput();
                    }
                }
                count += n;
            }
            return count;
        } catch (DataFormatException e) {
            String message = e.getMessage();
            throw new IOException(message != null ? message : "Invalid ZLib data format");
        }
    }

    @Override
    public void close() throws IOException {
        inflater.end();
        base.close();
        super.close();
    }

    @Override
    public void restart() throws IOException {
        ensureOpen();
        base.restart();
        inflater.reset();
        inputLength = 0;
    }

    private void fillInput() throws IOException {
        inputLength = base.read(inputBuffer);
        if (inputLength < 0) {
            throw new IOException("Unexpected end of ZLib input stream");
        } else {
            inflater.setInput(inputBuffer, 0, inputLength);
        }
    }
}
