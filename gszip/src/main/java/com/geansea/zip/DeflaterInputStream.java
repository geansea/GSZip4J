package com.geansea.zip;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.zip.Deflater;

final class DeflaterInputStream extends GsZipInputStream {
    @NonNull
    private final GsZipInputStream base;
    @NonNull
    private final Deflater deflater;
    @NonNull
    private final byte[] inputBuffer;
    private int inputLength;

    DeflaterInputStream(@NonNull GsZipInputStream base) throws IOException {
        this.base = base;
        deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        inputBuffer = new byte[GsZipUtil.BUFFER_SIZE];
        restart();
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return deflater.finished() ? 0 : 1;
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (len == 0) {
            return 0;
        }
        if (deflater.finished()) {
            return -1;
        }
        int count = 0;
        while (count == 0) {
            if (deflater.finished()) {
                break;
            }
            if (deflater.needsInput()) {
                fillInput();
            }
            count = deflater.deflate(b, off, len);
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        deflater.end();
        base.close();
        super.close();
    }

    @Override
    public void restart() throws IOException {
        ensureOpen();
        base.restart();
        deflater.reset();
        inputLength = 0;
    }

    private void fillInput() throws IOException {
        inputLength = base.read(inputBuffer);
        if (inputLength < 0) {
            deflater.finish();
        } else {
            deflater.setInput(inputBuffer, 0, inputLength);
        }
    }
}
