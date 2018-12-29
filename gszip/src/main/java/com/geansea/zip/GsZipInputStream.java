package com.geansea.zip;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Sub-class of InputStream to add some useful methods.
 */
public class GsZipInputStream extends InputStream {
    private boolean closed = false;

    @Override
    public int read() throws IOException {
        ensureOpen();
        byte[] buffer = new byte[1];
        int count = read(buffer);
        return (count > 0 ? (buffer[0] & 0xFF) : -1);
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        throw new IOException("This method should be overridden by subclasses");
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }

    /**
     * Reset the stream to the start.
     * @throws IOException if not supported or
     */
    public void restart() throws IOException {
        throw new IOException("Restart is not supported");
    }

    void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }
}
