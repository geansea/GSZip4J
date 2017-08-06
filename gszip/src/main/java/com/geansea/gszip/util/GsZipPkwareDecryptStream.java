package com.geansea.gszip.util;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class GsZipPkwareDecryptStream extends InputStream {
    private static final int HEADER_LEN = 12;

    private final InputStream base;
    private final byte[] password;
    private final byte crcCheck;
    private final byte timeCheck;
    private GsZipPkwareKey key;

    public GsZipPkwareDecryptStream(@NonNull InputStream base,
                                    @NonNull byte[] password,
                                    byte timeCheck,
                                    byte crcCheck) throws IOException {
        GsZipUtil.check(password.length > 0, "Empty password");
        this.base = base;
        this.password = password;
        this.crcCheck = crcCheck;
        this.timeCheck = timeCheck;
        initKey();
    }

    private void initKey() throws IOException {
        key = new GsZipPkwareKey();
        for (byte c : password) {
            key.update(c);
        }

        byte[] header = new byte[HEADER_LEN];
        GsZipUtil.check(base.read(header) == header.length, "Read header fail");
        byte check = 0;
        for (byte c : header) {
            c ^= key.cryptByte();
            key.update(c);
            check = c;
        }
        GsZipUtil.check(check == crcCheck || check == timeCheck, "Check fail");
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
    public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
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
