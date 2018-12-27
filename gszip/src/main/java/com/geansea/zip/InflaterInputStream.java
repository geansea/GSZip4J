package com.geansea.zip;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class InflaterInputStream extends GsZipInputStream {
    @NonNull
    private final GsZipInputStream base;
    @NonNull
    private final Inflater inflater;
    @NonNull
    private final byte[] inputBuffer;
    private int inputLength;

    InflaterInputStream(@NonNull GsZipInputStream base) throws IOException {
        this.base = base;
        inflater = new Inflater(true);
        inputBuffer = new byte[GsZipUtil.BUFFER_SIZE];
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
        return count > 0 ? (buffer[0] & 0xFF) : -1;
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (len == 0) {
            return 0;
        }
        if (inflater.finished()) {
            return -1;
        }
        int count = 0;
        while (count == 0) {
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
            try {
                count = inflater.inflate(b, off, len);
            } catch (DataFormatException e) {
                String message = e.getMessage();
                throw new IOException(message != null ? message : "Invalid ZLib data format");
            }
        }
        return count;
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
