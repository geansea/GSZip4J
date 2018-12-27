package com.geansea.zip;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.Random;

final class PKWareEncryptInputStream extends GsZipInputStream {
    private static final int HEADER_LEN = 12;

    private final GsZipInputStream base;
    private final byte[] password;
    private final PKWareKey key;
    private final byte[] rawHeader;
    private final byte[] header;
    private int headerPos;

    PKWareEncryptInputStream(@NonNull GsZipInputStream base,
                             byte @NonNull [] password,
                             byte checkByte) throws IOException {
        this.base = base;
        this.password = password;
        key = new PKWareKey();
        rawHeader = new byte[HEADER_LEN];
        header = new byte[HEADER_LEN];
        // Init raw header
        Random random = new Random();
        random.nextBytes(rawHeader);
        rawHeader[rawHeader.length - 1] = checkByte;
        restart();
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        if (headerPos < header.length) {
            return 1;
        }
        return base.available();
    }

    @Override
    public int read() throws IOException {
        ensureOpen();
        byte[] buffer = new byte[1];
        int count = read(buffer);
        return count > 0 ? (buffer[0] & 0xFF) : -1;
    }

    @Override
    public int read(byte @NonNull [] b,
                    @NonNegative int off,
                    @NonNegative int len) throws IOException {
        ensureOpen();
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
        headerPos = 0;
        // Update key with password
        for (byte c : password) {
            key.update(c);
        }
        // Update key and header
        for (int i = 0; i < header.length; ++i) {
            header[i] = (byte) (rawHeader[i] ^ key.cryptByte());
            key.update(rawHeader[i]);
        }
    }
}
