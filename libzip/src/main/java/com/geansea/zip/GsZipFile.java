package com.geansea.zip;

import com.geansea.zip.util.GsZipCentralDirEnd;
import com.geansea.zip.util.GsZipEntryHeader;
import com.geansea.zip.util.GsZipPkwareDecryptStream;
import com.geansea.zip.util.GsZipSubStream;
import com.geansea.zip.util.GsZipUtil;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class GsZipFile {
    private final @NonNull String name;
    private @NonNull String password;
    private @NonNull RandomAccessFile file;
    private @NonNull GsZipCentralDirEnd dirEnd;
    private @NonNull String comment;
    private @NonNull ArrayList<GsZipEntry> entryList;
    private @NonNull GsZipEntryNode entryTree;

    public static @Nullable GsZipFile create(@NonNull String path) {
        GsZipFile zipFile = null;
        try {
            zipFile = new GsZipFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipFile;
    }

    private GsZipFile(@NonNull String path) throws IOException {
        name = path;
        password = "";
        file = new RandomAccessFile(name, "r");
        dirEnd = new GsZipCentralDirEnd();
        comment = "";
        entryList = new ArrayList<>();
        entryTree = new GsZipEntryNode(null, "");
        readCentralDirEnd();
        readCentralDir();
    }

    public boolean needPassword() {
        boolean encrypted = false;
        for (GsZipEntry entry : entryList) {
            if (GsZipEntry.EncryptMethod.NONE != entry.getEncryptMethod()) {
                encrypted = true;
                break;
            }
        }
        return encrypted;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
    }

    public @NonNull String getName() {
        return name;
    }

    public int size() {
        return entryList.size();
    }

    public @NonNull String getComment() {
        return comment;
    }

    public @Nullable GsZipEntry getEntry(int index) {
        GsZipEntry entry = null;
        if (0 <= index && index < size()) {
            entry = entryList.get(index);
        }
        return entry;
    }

    public @Nullable InputStream getInputStream(int index) {
        try {
            GsZipEntry entry = getEntry(index);
            GsZipUtil.check(entry != null, "Error index");
            GsZipUtil.check(entry.isFile(), "Only for file");
            synchronized (this) {
                GsZipSubStream subStream = new GsZipSubStream(file, entry.getLocalOffset());
                GsZipEntryHeader localHeader = new GsZipEntryHeader();
                localHeader.readFrom(subStream, false);
                GsZipEntry localEntry = new GsZipEntry(0, localHeader, StandardCharsets.UTF_8);
                GsZipUtil.check(entry.matchLocal(localEntry), "Entry header mismatch");
                subStream.resetSize(entry.getCompressedSize());

                InputStream decryptStream = null;
                if (entry.getEncryptMethod() == GsZipEntry.EncryptMethod.NONE) {
                    decryptStream = subStream;
                } else if (entry.getEncryptMethod() == GsZipEntry.EncryptMethod.PKWARE) {
                    GsZipUtil.check(!password.isEmpty(), "Need password");
                    byte[] pwBytes = password.getBytes(StandardCharsets.UTF_8);
                    byte timeCheck = entry.getTimeCheck();
                    byte crcCheck = entry.getCrcCheck();
                    decryptStream = new GsZipPkwareDecryptStream(subStream, pwBytes, timeCheck, crcCheck);
                } else {
                    GsZipUtil.check(false, "Not supported encrypt method");
                }

                InputStream uncompStream = null;
                if (entry.getCompressMethod() == GsZipEntry.CompressMethod.STORED) {
                    uncompStream = decryptStream;
                } else if (entry.getCompressMethod() == GsZipEntry.CompressMethod.FLATE) {
                    uncompStream = new InflaterInputStream(decryptStream, new Inflater(true));
                } else {
                    GsZipUtil.check(false, "Not supported compress method");
                }

                return uncompStream;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public @Nullable GsZipEntry getEntry(String path) {
        GsZipEntryNode node = entryTree.getChildWithPath(path);
        return ((node != null) ? node.getEntry() : null);
    }

    public @Nullable InputStream getInputStream(String path) {
        GsZipEntry entry = getEntry(path);
        return ((entry != null) ? getInputStream(entry.getIndex()) : null);
    }

    public @NonNull GsZipEntryNode getEntryTree() {
        return entryTree;
    }

    private void readCentralDirEnd() throws IOException {
        long scanOffset = file.length() - GsZipCentralDirEnd.BASE_SIZE;
        GsZipUtil.check(scanOffset >= 0, "File too short to be a zip file");

        long stopOffset = Math.max(scanOffset - 0xFFFF, 0);
        long dirEndOffset = -1;
        while (stopOffset <= scanOffset) {
            file.seek(scanOffset);
            if (Integer.reverseBytes(file.readInt()) == GsZipCentralDirEnd.MAGIC) {
                dirEndOffset = scanOffset;
                break;
            }
            scanOffset--;
        }

        GsZipUtil.check(dirEndOffset >= 0, "Find central dir fail");
        int eocdSize = (int) (file.length() - dirEndOffset);
        byte[] bytes = new byte[eocdSize];
        file.seek(dirEndOffset);
        file.read(bytes);

        dirEnd.readFrom(bytes);
        comment = dirEnd.getComment(StandardCharsets.UTF_8);
    }

    private void readCentralDir() throws IOException {
        int dirOffset = dirEnd.getDirOffset();
        int dirSize = dirEnd.getDirSize();
        GsZipSubStream rafStream = new GsZipSubStream(file, dirOffset, dirOffset + dirSize);

        int entryCount = dirEnd.getEntryCount();
        entryList.ensureCapacity(entryCount);
        for (int i = 0; i < entryCount; ++i) {
            GsZipEntryHeader header = new GsZipEntryHeader();
            header.readFrom(rafStream, true);

            GsZipEntry entry = new GsZipEntry(i, header, StandardCharsets.UTF_8);
            entryList.add(entry);
            entryTree.addChild(entry.getName(), entry);
        }
    }
}
