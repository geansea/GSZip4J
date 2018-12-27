package com.geansea.zip;

import android.support.annotation.NonNull;

import java.nio.charset.Charset;
import java.util.Date;

public class GsZipEntry {
    enum CompressMethod {
        STORED,
        FLATE,
        NONSUPPORT
    }

    enum EncryptMethod {
        NONE,
        PKWARE,
        NONSUPPORT
    }

    private final int index;
    @NonNull
    private final EntryHeader header;
    @NonNull
    private final String name;
    @NonNull
    private final Date time;

    GsZipEntry(int index, @NonNull EntryHeader header, @NonNull Charset charset) {
        this.index = index;
        this.header = header;
        name = header.getFileName(charset);
        time = header.getLastModifiedTime();
    }

    public int getIndex() {
        return index;
    }

    boolean matchLocal(@NonNull GsZipEntry entry) {
        return header.matchLocal(entry.header);
    }

    @NonNull
    public String getName() {
        return name;
    }

    public boolean isFile() {
        return !(name.endsWith("/") && header.getUncompSize() == 0);
    }

    @NonNull
    EncryptMethod getEncryptMethod() {
        switch (header.getEncMethod()) {
            case EntryHeader.ENCRYPT_NONE:
                return EncryptMethod.NONE;
            case EntryHeader.ENCRYPT_PKWARE:
                return EncryptMethod.PKWARE;
            default:
                return EncryptMethod.NONSUPPORT;
        }
    }

    public boolean isEncrypted() {
        return (getEncryptMethod() != EncryptMethod.NONE);
    }

    @NonNull
    CompressMethod getCompressMethod() {
        switch (header.getCompMethod()) {
            case EntryHeader.COMPRESS_STORED:
                return CompressMethod.STORED;
            case EntryHeader.COMPRESS_FLATE:
                return CompressMethod.FLATE;
            default:
                return CompressMethod.NONSUPPORT;
        }
    }

    public boolean isCompressed() {
        return (getCompressMethod() != CompressMethod.STORED);
    }

    @NonNull
    public Date getTime() {
        return time;
    }

    public int getCRC() {
        return header.getCRC();
    }

    byte getTimeCheck() {
        return header.getTimeCheck();
    }

    byte getCrcCheck() {
        return header.getCrcCheck();
    }

    public int getCompressedSize() {
        return header.getCompSize();
    }

    public int getOriginalSize() {
        return header.getUncompSize();
    }

    int getLocalOffset() {
        return header.getLocalOffset();
    }
}
