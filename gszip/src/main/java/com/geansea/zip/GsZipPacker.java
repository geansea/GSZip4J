package com.geansea.zip;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class GsZipPacker {
    @NonNull
    private final LinkedList<EntryInfo> entryList;
    @NonNull
    private final LinkedHashMap<String, String> entries;
    @NonNull
    private Charset defaultCharset;
    @NonNull
    private String comment;

    public GsZipPacker() {
        entryList = new LinkedList<>();
        entries = new LinkedHashMap<>();
        defaultCharset = StandardCharsets.UTF_8;
        comment = "";
    }

    public void setDefaultCharset(@NonNull Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

    public boolean addFile(@NonNull String entryName, @NonNull String fileName) {
        try {
            entryName = GsZipUtil.normalizePath(entryName);
            GsZipUtil.check(!entryName.isEmpty(), "Empty entry name");
            GsZipUtil.check(new File(fileName).isFile(), "Invalid file name");

            GsZipUtil.check(!entries.containsKey(entryName), "Already has entry");
            String parentName = GsZipUtil.getParentPath(entryName);
            if (!parentName.isEmpty()) {
                GsZipUtil.check(addFolder(parentName), "Add parent folder fail");
            }

            EntryInfo info = new EntryInfo(entryName, fileName);
            entryList.add(info);
            entries.put(entryName, fileName);
            return true;
        } catch (GsZipException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addFolder(@NonNull String entryName) {
        try {
            entryName = GsZipUtil.normalizePath(entryName);
            GsZipUtil.check(!entryName.isEmpty(), "Empty entry name");

            if (entries.containsKey(entryName)) {
                // Folder already added
                GsZipUtil.check(entries.get(entryName).isEmpty(), "Same name with file");
                return true;
            }

            String parentName = GsZipUtil.getParentPath(entryName);
            if (!parentName.isEmpty()) {
                GsZipUtil.check(addFolder(parentName), "Add parent folder fail");
            }

            EntryInfo info = new EntryInfo(entryName + "/", "");
            entryList.add(info);
            entries.put(entryName, "");
            return true;
        } catch (GsZipException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setComment(@NonNull String comment) {
        this.comment = comment;
    }

    public boolean packTo(@NonNull String filePath, @NonNull String password) {
        try {
            File file = new File(filePath);
            GsZipUtil.check(!file.exists(), "File already exist");
            FileOutputStream stream = new FileOutputStream(file);
            return packTo(stream, password);
        } catch (@NonNull IOException | GsZipException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean packTo(@NonNull OutputStream stream, @NonNull String password) {
        try {
            int streamOffset = 0;
            for (EntryInfo info : entryList) {
                EntryHeader header = info.header;
                header.setSign(false);
                header.setLocalOffset(streamOffset);

                // Folder
                if (info.path.isEmpty()) {
                    header.writeTo(stream, false);
                    streamOffset += header.byteSize(false);
                    continue;
                }

                // File
                RandomAccessFile file = new RandomAccessFile(info.path, "r");
                GsZipInputStream entryStream = new SubInputStream(file, 0);
                int crc = GsZipUtil.calcStreamCRC(entryStream);
                int origLength = GsZipUtil.calcStreamLength(entryStream);
                header.setCRC(crc);
                header.setCompSize(origLength);
                header.setUncompSize(origLength);

                if (origLength > 0) {
                    GsZipInputStream compStream = new DeflaterInputStream(entryStream);
                    int compLength = GsZipUtil.calcStreamLength(compStream);
                    if (compLength < origLength) {
                        header.setCompMethod(EntryHeader.COMPRESS_FLATE);
                        header.setCompSize(compLength);
                        entryStream = compStream;
                    }
                }

                if (!password.isEmpty()) {
                    byte[] pwBytes = password.getBytes(defaultCharset);
                    byte timeCheck = header.getTimeCheck();
                    // byte crcCheck = header.getCrcCheck();
                    GsZipInputStream encStream = new PKWareEncryptInputStream(entryStream, pwBytes, timeCheck);
                    int encLength = GsZipUtil.calcStreamLength(encStream);
                    header.setEncMethod(EntryHeader.ENCRYPT_PKWARE);
                    header.setCompSize(encLength);
                    entryStream = encStream;
                }

                header.writeTo(stream, false);
                streamOffset += header.byteSize(false);
                if (header.getCompSize() > 0) {
                    entryStream.restart();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = entryStream.read(buffer)) > 0) {
                        stream.write(buffer, 0, count);
                    }
                    streamOffset += header.getCompSize();
                }
            }

            int dirSize = 0;
            for (EntryInfo info : entryList) {
                EntryHeader header = info.header;
                header.setSign(true);
                header.writeTo(stream, true);
                dirSize += header.byteSize(true);
            }

            CentralDirEnd dirEnd = new CentralDirEnd();
            dirEnd.setEntryCount(entryList.size());
            dirEnd.setDirRange(streamOffset, dirSize);
            dirEnd.setComment(comment, defaultCharset);

            byte[] endBytes = new byte[dirEnd.byteSize()];
            dirEnd.writeTo(endBytes);
            stream.write(endBytes);
            return true;
        } catch (@NonNull IOException | GsZipException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class EntryInfo {
        @NonNull
        final String name;
        @NonNull
        final String path;
        @NonNull
        final EntryHeader header;

        EntryInfo(@NonNull String entryName, @NonNull String fileName) {
            name = entryName;
            path = fileName;
            header = new EntryHeader();
            header.setFileName(entryName);
            if (!path.isEmpty()) {
                long lastModTime = new File(fileName).lastModified();
                header.setLastModifiedTime(new Date(lastModTime));
            }
        }
    }
}
