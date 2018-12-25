package com.geansea.zip;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;

final class GsZipPKWareDecryptStream extends InputStream {
    private static final int HEADER_LEN = 12;

    private final InputStream base;
    private final GsZipPKWareKey key;

    public GsZipPKWareDecryptStream(@NonNull InputStream base,
                                    byte @NonNull [] password,
                                    byte timeCheck,
                                    byte crcCheck) throws IOException, GsZipException {
        this.base = base;
        key = new GsZipPKWareKey();
        // Update key with password
        for (byte c : password) {
            key.update(c);
        }
        // Update key with header
        byte[] header = new byte[HEADER_LEN];
        int readLen = base.read(header);
        GsZipUtil.check(readLen == header.length,
                "Read header from base stream failed");
        byte checkByte = 0;
        for (byte c : header) {
            c ^= key.cryptByte();
            key.update(c);
            checkByte = c;
        }
        GsZipUtil.check(checkByte == crcCheck || checkByte == timeCheck,
                "Check byte not matched");
    }

    @Override
    public int available() throws IOException {
        return base.available();
    }

    @Override
    public int read() throws IOException {
        int value = base.read();
        if (value >= 0) {
            byte c = (byte) value;
            c ^= key.cryptByte();
            key.update(c);
            return c;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte @NonNull [] buffer,
                    @NonNegative int byteOffset,
                    @NonNegative int byteCount) throws IOException {
        int count = base.read(buffer, byteOffset, byteCount);
        if (count >= 0) {
            for (int i = byteOffset; i < byteOffset + count; ++i) {
                buffer[i] ^= key.cryptByte();
                key.update(buffer[i]);
            }
            return count;
        } else {
            return -1;
        }
    }
}
