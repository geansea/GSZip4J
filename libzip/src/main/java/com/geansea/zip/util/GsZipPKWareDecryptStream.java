package com.geansea.zip.util;

import com.google.common.base.Preconditions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class GsZipPKWareDecryptStream extends InputStream {
    private static final int HEADER_LEN = 12;

    private final InputStream base;
    private final byte[] password;
    private final byte crcCheck;
    private final byte timeCheck;
    private GsZipPKWareKey key;

    public GsZipPKWareDecryptStream(@NonNull InputStream base,
                                    byte @NonNull [] password,
                                    byte timeCheck,
                                    byte crcCheck) throws IOException, IllegalArgumentException, IllegalStateException {
        Preconditions.checkArgument(password.length > 0, "Empty password");
        this.base = base;
        this.password = password;
        this.crcCheck = crcCheck;
        this.timeCheck = timeCheck;
        key = new GsZipPKWareKey();
        for (byte c : password) {
            key.update(c);
        }

        byte[] header = new byte[HEADER_LEN];
        Preconditions.checkState(base.read(header) == header.length, "Read header fail");
        byte check = 0;
        for (byte c : header) {
            c ^= key.cryptByte();
            key.update(c);
            check = c;
        }
        Preconditions.checkState(check == crcCheck || check == timeCheck, "Check fail");
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
    public int read(byte @NonNull [] buffer, int byteOffset, int byteCount) throws IOException {
        int count = base.read(buffer, byteOffset, byteCount);
        if (count > 0) {
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
