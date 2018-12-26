package com.geansea.zip;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class GsZipPKWareEncryptStream extends InputStream {
    private static final int HEADER_LEN = 12;

    private final InputStream base;
    private final PKWareKey key;
    private final byte[] header;
    private int headerPos;

    public GsZipPKWareEncryptStream(@NonNull InputStream base,
                                    byte @NonNull [] password,
                                    byte checkByte) throws IllegalArgumentException {
        this.base = base;
        key = new PKWareKey();
        header = new byte[HEADER_LEN];
        headerPos = 0;
        // Update key with password
        for (byte c : password) {
            key.update(c);
        }
        // Init header
        Random random = new Random();
        random.nextBytes(header);
        header[header.length - 1] = checkByte;
        // Update key and header
        for (int i = 0; i < header.length; ++i) {
            byte c = header[i];
            header[i] ^= key.cryptByte();
            key.update(c);
        }
    }

    @Override
    public int available() throws IOException {
        if (headerPos < header.length) {
            return header.length - headerPos;
        }
        return base.available();
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int count = read(buffer);
        return (count > 0 ? Byte.toUnsignedInt(buffer[0]) : -1);
    }

    @Override
    public int read(byte @NonNull [] b,
                    @NonNegative int off,
                    @NonNegative int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (headerPos < header.length) {
            int count = Math.min(header.length - headerPos, len);
            System.arraycopy(header, headerPos, b, off, count);
            headerPos += count;
            return count;
        }
        int count = base.read(b, off, len);
        if (count > 0) {
            for (int i = off; i < off + count; ++i) {
                byte c = b[i];
                b[i] ^= key.cryptByte();
                key.update(c);
            }
        }
        return count;
    }
}
