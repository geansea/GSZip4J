package com.geansea.zip;

import com.geansea.zip.util.GsZipCentralDirEnd;
import com.geansea.zip.util.GsZipEntryHeader;
import com.geansea.zip.util.GsZipPkwareEncryptStream;
import com.geansea.zip.util.GsZipUtil;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
                InputStream entryStream = new FileInputStream(info.path);
                entryStream = new BufferedInputStream(entryStream);
                entryStream.mark(Integer.MAX_VALUE);
                int origLength = GsZipUtil.getStreamLength(entryStream);
                header.setCRC(GsZipUtil.getStreamCRC(entryStream));
                header.setCompSize(origLength);
                header.setUncompSize(origLength);

                if (origLength > 0) {
                    InputStream compStream = new DeflaterInputStream(entryStream, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
                    compStream = new BufferedInputStream(compStream);
                    compStream.mark(Integer.MAX_VALUE);
                    int compLength = GsZipUtil.getStreamLength(compStream);
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
                    InputStream encStream = new GsZipPkwareEncryptStream(entryStream, pwBytes, timeCheck);
                    encStream = new BufferedInputStream(encStream);
                    encStream.mark(Integer.MAX_VALUE);
                    int encLength = GsZipUtil.getStreamLength(encStream);
                    header.setEncMethod(GsZipEntryHeader.ENCRYPT_PKWARE);
                    header.setCompSize(encLength);
                    entryStream = encStream;
                }

                header.writeTo(stream, false);
                streamOffset += header.byteSize(false);
                if (header.getCompSize() > 0) {
                    entryStream.reset();
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

            GsZipCentralDirEnd dirEnd = new GsZipCentralDirEnd();
            dirEnd.setEntryCount(entryList.size());
            dirEnd.setDirRange(streamOffset, dirSize);
            dirEnd.setComment(comment, StandardCharsets.UTF_8);
            dirEnd.setComment(comment, StandardCharsets.UTF_8);

            byte[] endBytes = new byte[dirEnd.byteSize()];
            dirEnd.writeTo(endBytes);
            stream.write(endBytes);
            return true;
        } catch (IOException e) {
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
            if (isFile()) {
                long lastModTime = new File(fileName).lastModified();
                header.setLastModifiedTime(new Date(lastModTime));
            }
        }

        boolean isFile() {
            return path.isEmpty();
        }
    }
}
