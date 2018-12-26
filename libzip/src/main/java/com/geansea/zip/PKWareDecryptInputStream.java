package com.geansea.zip;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;

final class PKWareDecryptInputStream extends GsZipInputStream {
    private static final int HEADER_LEN = 12;

    private final GsZipInputStream base;
    private final byte[] password;
    private final PKWareKey key;
    private final byte[] header;

    PKWareDecryptInputStream(@NonNull GsZipInputStream base,
                             byte @NonNull [] password,
                             byte timeCheck,
                             byte crcCheck) throws IOException, GsZipException {
        this.base = base;
        this.password = password;
        key = new PKWareKey();
        header = new byte[HEADER_LEN];
        restart();
        byte checkByte = header[header.length - 1];
        GsZipUtil.check(checkByte == crcCheck || checkByte == timeCheck,
                "Check byte not matched, the password maybe incorrect");
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return base.available();
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        int value = base.read();
        if (value >= 0) {
            byte c = (byte) value;
            c ^= key.cryptByte();
            key.update(c);
            return Byte.toUnsignedInt(c);
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte @NonNull [] b,
                    @NonNegative int off,
                    @NonNegative int len) throws IOException {
        ensureOpen();
        int count = base.read(b, off, len);
        if (count >= 0) {
            for (int i = off; i < off + count; ++i) {
                b[i] ^= key.cryptByte();
                key.update(b[i]);
            }
            return count;
        } else {
            return -1;
        }
    }

    @Override
    public void close() throws IOException {
        base.close();
        super.close();
    }

    @Override
    public void restart() throws IOException {
        ensureOpen();
        base.restart();
        key.reset();
        // Update key with password
        for (byte c : password) {
            key.update(c);
        }
        // Update key with header
        int readLen = base.read(header);
        if (readLen != header.length) {
            throw new IOException("Read header from base stream failed");
        }
        for (int i = 0; i < header.length; ++i) {
            header[i] ^= key.cryptByte();
            key.update(header[i]);
        }
    }
}
