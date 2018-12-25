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
        if (headerPos < header.length) {
            return header[headerPos++];
        }
        int value = base.read();
        if (value >= 0) {
            byte c = (byte) value;
            value ^= key.cryptByte();
            key.update(c);
            return value;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte @NonNull [] buffer,
                    @NonNegative int byteOffset,
                    @NonNegative int byteCount) throws IOException {
        int headerCount = 0;
        if (headerPos < header.length) {
            headerCount = Math.min(header.length - headerPos, byteCount);
            System.arraycopy(header, headerPos, buffer, byteOffset, headerCount);
            headerPos += headerCount;
        }
        if (headerCount >= byteCount) {
            return headerCount;
        }
        int count = base.read(buffer, byteOffset + headerCount, byteCount - headerCount);
        if (count >= 0) {
            for (int i = byteOffset; i < byteOffset + count; ++i) {
                byte c = buffer[i];
                buffer[i] ^= key.cryptByte();
                key.update(c);
            }
            return headerCount + count;
        } else if (headerCount > 0) {
            return headerCount;
        } else {
            return -1;
        }
    }
}
