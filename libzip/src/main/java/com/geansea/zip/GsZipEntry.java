package com.geansea.zip;

import com.geansea.zip.util.GsZipEntryHeader;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

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
    private final @NonNull GsZipEntryHeader header;
    private final @NonNull String name;
    private final @NonNull Date time;

    GsZipEntry(@NonNegative int index, @NonNull GsZipEntryHeader header, @NonNull Charset charset) {
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

    public @NonNull String getName() {
        return name;
    }

    public boolean isFile() {
        return !(name.endsWith("/") && header.getUncompSize() == 0);
    }

    EncryptMethod getEncryptMethod() {
        switch (header.getEncMethod()) {
            case GsZipEntryHeader.ENCRYPT_NONE:
                return EncryptMethod.NONE;
            case GsZipEntryHeader.ENCRYPT_PKWARE:
                return EncryptMethod.PKWARE;
            default:
                return EncryptMethod.NONSUPPORT;
        }
    }

    public boolean isEncrypted() {
        return (getEncryptMethod() != EncryptMethod.NONE);
    }

    CompressMethod getCompressMethod() {
        switch (header.getCompMethod()) {
            case GsZipEntryHeader.COMPRESS_STORED:
                return CompressMethod.STORED;
            case GsZipEntryHeader.COMPRESS_FLATE:
                return CompressMethod.FLATE;
            default:
                return CompressMethod.NONSUPPORT;
        }
    }

    public boolean isCompressed() {
        return (getCompressMethod() != CompressMethod.STORED);
    }

    public @NonNull Date getTime() {
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
