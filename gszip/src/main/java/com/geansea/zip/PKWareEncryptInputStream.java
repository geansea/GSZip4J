package com.geansea.zip;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Random;

final class PKWareEncryptInputStream extends GsZipInputStream {
    private static final int HEADER_LEN = 12;

    @NonNull
    private final GsZipInputStream base;
    @NonNull
    private final byte[] password;
    @NonNull
    private final PKWareKey key;
    @NonNull
    private final byte[] rawHeader;
    @NonNull
    private final byte[] header;
    private int headerPos;

    PKWareEncryptInputStream(@NonNull GsZipInputStream base,
                             @NonNull byte[] password,
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
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
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
