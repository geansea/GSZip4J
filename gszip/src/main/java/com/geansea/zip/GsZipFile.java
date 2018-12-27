package com.geansea.zip;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class GsZipFile {
    @NonNull
    private final RandomAccessFile file;
    @NonNull
    private final CentralDirEnd dirEnd;
    @NonNull
    private final ArrayList<GsZipEntry> entryList;
    @NonNull
    private final GsZipEntryNode entryTree;
    @NonNull
    private Charset defaultCharset;
    @NonNull
    private String password;

    @NonNull
    public static GsZipFile create(@NonNull String path) throws GsZipException {
        try {
            GsZipFile zip = new GsZipFile(path);
            zip.readCentralDirEnd();
            zip.readCentralDir();
            return zip;
        } catch (IOException e) {
            String message = e.getMessage();
            throw new GsZipException(message != null ? message : "Create zip file failed");
        }
    }

    private GsZipFile(@NonNull String path) throws IOException {
        file = new RandomAccessFile(path, "r");
        dirEnd = new CentralDirEnd();
        entryList = new ArrayList<>();
        entryTree = new GsZipEntryNode(null, "");
        defaultCharset = StandardCharsets.UTF_8;
        password = "";
    }

    public void setDefaultCharset(@NonNull Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
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

    public int size() {
        return entryList.size();
    }

    @NonNull
    public String getComment() {
        return dirEnd.getComment(defaultCharset);
    }

    @NonNull
    public GsZipEntry getEntry(int index) throws GsZipException {
        GsZipUtil.check(index >= 0, "");
        GsZipUtil.check(index < entryList.size(), "");
        return entryList.get(index);
    }

    @NonNull
    public GsZipInputStream getInputStream(int index) throws GsZipException {
        GsZipEntry entry = getEntry(index);
        if (!entry.isFile()) {
            return new GsZipInputStream();
        }
        synchronized (this) {
            try {
                GsZipInputStream entryStream = new SubInputStream(file, entry.getLocalOffset());
                EntryHeader localHeader = new EntryHeader();
                localHeader.readFrom(entryStream, false);
                GsZipEntry localEntry = new GsZipEntry(0, localHeader, StandardCharsets.UTF_8);
                GsZipUtil.check(entry.matchLocal(localEntry), "Entry header mismatch");

                long offset = entry.getLocalOffset() + localHeader.byteSize(false);
                GsZipInputStream subStream = new SubInputStream(file, offset, offset + localHeader.getCompSize());
                GsZipInputStream decryptStream;
                if (entry.getEncryptMethod() == GsZipEntry.EncryptMethod.NONE) {
                    decryptStream = subStream;
                } else if (entry.getEncryptMethod() == GsZipEntry.EncryptMethod.PKWARE) {
                    GsZipUtil.check(!password.isEmpty(), "Need password");
                    byte[] pwBytes = password.getBytes(StandardCharsets.UTF_8);
                    byte timeCheck = entry.getTimeCheck();
                    byte crcCheck = entry.getCrcCheck();
                    decryptStream = new PKWareDecryptInputStream(subStream, pwBytes, timeCheck, crcCheck);
                } else {
                    throw new IOException("Not supported encrypt method");
                }

                GsZipInputStream uncompressStream;
                if (entry.getCompressMethod() == GsZipEntry.CompressMethod.STORED) {
                    uncompressStream = decryptStream;
                } else if (entry.getCompressMethod() == GsZipEntry.CompressMethod.FLATE) {
                    uncompressStream = new InflaterInputStream(decryptStream);
                } else {
                    throw new IOException("Not supported compress method");
                }

                return uncompressStream;
            } catch (IOException e) {
                String message = e.getMessage();
                throw new GsZipException(message != null ? message : "Get entry stream failed");
            }
        }
    }

    @Nullable
    public GsZipEntry getEntry(@NonNull String path) {
        GsZipEntryNode node = entryTree.getChildWithPath(path);
        return ((node != null) ? node.getEntry() : null);
    }

    @Nullable
    public GsZipInputStream getInputStream(@NonNull String path) throws GsZipException {
        GsZipEntry entry = getEntry(path);
        return ((entry != null) ? getInputStream(entry.getIndex()) : null);
    }

    @NonNull
    public GsZipEntryNode getEntryTree() {
        return entryTree;
    }

    private void readCentralDirEnd() throws IOException, GsZipException {
        long scanOffset = file.length() - CentralDirEnd.BASE_SIZE;
        GsZipUtil.check(scanOffset >= 0, "File too short to be a zip file");

        long stopOffset = Math.max(scanOffset - 0xFFFF, 0);
        long dirEndOffset = -1;
        while (stopOffset <= scanOffset) {
            file.seek(scanOffset);
            if (Integer.reverseBytes(file.readInt()) == CentralDirEnd.MAGIC) {
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
    }

    private void readCentralDir() throws IOException, GsZipException {
        long dirOffset = dirEnd.getDirOffset();
        long dirSize = dirEnd.getDirSize();
        SubInputStream rafStream = new SubInputStream(file, dirOffset, dirOffset + dirSize);

        int entryCount = dirEnd.getEntryCount();
        entryList.ensureCapacity(entryCount);
        for (int i = 0; i < entryCount; ++i) {
            EntryHeader header = new EntryHeader();
            header.readFrom(rafStream, true);

            GsZipEntry entry = new GsZipEntry(i, header, defaultCharset);
            entryList.add(entry);
            entryTree.addChild(entry.getName(), entry);
        }
    }
}
