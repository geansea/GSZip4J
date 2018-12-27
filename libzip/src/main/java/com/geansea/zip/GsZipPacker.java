package com.geansea.zip;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class GsZipPacker {
    private @NonNull String comment;
    private @NonNull LinkedList<EntryInfo> entryList;
    private @NonNull LinkedHashMap<String, String> entries;

    public GsZipPacker() {
        comment = "";
        entryList = new LinkedList<>();
        entries = new LinkedHashMap<>();
    }

    public void setComment(@NonNull String comment) {
        this.comment = comment;
    }

    public boolean addFile(@NonNull String entryName, @NonNull String fileName) {
        try {
            entryName = GsZipUtil.getCanonicalPath(entryName);
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
            entryName = GsZipUtil.getCanonicalPath(entryName);
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

    public boolean packTo(@NonNull String filePath, @NonNull String password) {
        try {
            File file = new File(filePath);
            GsZipUtil.check(!file.exists(), "File already exist");
            FileOutputStream stream = new FileOutputStream(file);
            return packTo(stream, password);
        } catch (IOException | GsZipException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean packTo(@NonNull OutputStream stream, @NonNull String password) {
        try {
            int streamOffset = 0;
            for (EntryInfo info : entryList) {
                GsZipEntryHeader header = info.header;
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
                        header.setCompMethod(GsZipEntryHeader.COMPRESS_FLATE);
                        header.setCompSize(compLength);
                        entryStream = compStream;
                    }
                }

                if (!password.isEmpty()) {
                    byte[] pwBytes = password.getBytes(StandardCharsets.UTF_8);
                    byte timeCheck = header.getTimeCheck();
                    // byte crcCheck = header.getCrcCheck();
                    GsZipInputStream encStream = new PKWareEncryptInputStream(entryStream, pwBytes, timeCheck);
                    int encLength = GsZipUtil.calcStreamLength(encStream);
                    header.setEncMethod(GsZipEntryHeader.ENCRYPT_PKWARE);
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
                GsZipEntryHeader header = info.header;
                header.setSign(true);
                header.writeTo(stream, true);
                dirSize += header.byteSize(true);
            }

            CentralDirEnd dirEnd = new CentralDirEnd();
            dirEnd.setEntryCount(entryList.size());
            dirEnd.setDirRange(streamOffset, dirSize);
            dirEnd.setComment(comment, StandardCharsets.UTF_8);
            dirEnd.setComment(comment, StandardCharsets.UTF_8);

            byte[] endBytes = new byte[dirEnd.byteSize()];
            dirEnd.writeTo(endBytes);
            stream.write(endBytes);
            return true;
        } catch (IOException | GsZipException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class EntryInfo {
        final @NonNull String name;
        final @NonNull String path;
        final @NonNull GsZipEntryHeader header;

        EntryInfo(@NonNull String entryName, @NonNull String fileName) {
            name = entryName;
            path = fileName;
            header = new GsZipEntryHeader();
            header.setFileName(entryName);
            if (!path.isEmpty()) {
                long lastModTime = new File(fileName).lastModified();
                header.setLastModifiedTime(new Date(lastModTime));
            }
        }
    }
}
