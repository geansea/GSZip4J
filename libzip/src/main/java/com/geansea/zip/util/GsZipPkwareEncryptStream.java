package com.geansea.zip.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class GsZipPkwareEncryptStream extends InputStream {
    private static final int HEADER_LEN = 12;

    private final InputStream base;
    private final byte[] password;
    private final byte check;
    private GsZipPkwareKey key;
    private byte[] header;
    private int headerPos;

    public GsZipPkwareEncryptStream(@NonNull InputStream base,
                                    byte @NonNull [] password,
                                    byte check) throws IOException {
        GsZipUtil.check(password.length > 0, "Empty password");
        this.base = base;
        this.password = password;
        this.check = check;
        initKey();
    }

    private void initKey() {
        key = new GsZipPkwareKey();
        for (byte c : password) {
            key.update(c);
        }

        header = new byte[HEADER_LEN];
        Random random = new Random();
        random.nextBytes(header);
        header[header.length - 1] = check;
        for (int i = 0; i < header.length; ++i) {
            byte c = header[i];
            header[i] ^= key.cryptByte();
            key.update(c);
        }
        headerPos = 0;
    }

    @Override
    public int available() throws IOException {
        return (HEADER_LEN - headerPos) + base.available();
    }

    @Override
    public int read() throws IOException {
        if (headerPos < HEADER_LEN) {
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
    public int read(byte @NonNull [] buffer, int byteOffset, int byteCount) throws IOException {
        if (headerPos < HEADER_LEN) {
            int count = Math.min(HEADER_LEN - headerPos, byteCount);
            System.arraycopy(header, headerPos, buffer, byteOffset, count);
            headerPos += count;
            return count;
        }
        int count = base.read(buffer, byteOffset, byteCount);
        if (count > 0) {
            for (int i = byteOffset; i < byteOffset + count; ++i) {
                byte c = buffer[i];
                buffer[i] ^= key.cryptByte();
                key.update(c);
            }
            return count;
        } else {
            return -1;
        }
    }
}
