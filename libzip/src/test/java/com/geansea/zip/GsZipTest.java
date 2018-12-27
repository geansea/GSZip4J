package com.geansea.zip;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

public class GsZipTest {
    private static void folderCheck(@NonNull String path) throws Exception {
        File file = new File(path);
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
    }

    private static void fileCheck(@NonNull String path, int size, int crc) throws Exception {
        File file = new File(path);
        assertTrue(file.exists());
        assertTrue(file.isFile());

        InputStream stream = new FileInputStream(file);
        stream = new BufferedInputStream(stream);
        stream.mark(Integer.MAX_VALUE);
        assertEquals(size, GsZipUtil.getStreamLength(stream));
        assertEquals(crc, GsZipUtil.getStreamCRC(stream));
    }

    @Test
    public void unpack() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL zipUrl = classLoader.getResource("flate.zip");
        String zipPath = zipUrl.getPath();

        File temp = File.createTempFile("GsZipTest", ".tmp");
        String dirPath = temp.getAbsolutePath() + ".d/";

        assertTrue(GsZip.unpackToFolder(zipPath, dirPath, ""));

        fileCheck(dirPath + "file_0.txt", 0, 0);
        fileCheck(dirPath + "file_1.txt", 256, 0xe42ddc29);
        folderCheck(dirPath + "empty");
        folderCheck(dirPath + "sub");
        fileCheck(dirPath + "sub/file_2.txt", 256, 0x996bcc1b);
    }

    @Test
    public void unpack_enc() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL zipUrl = classLoader.getResource("flate_enc.zip");
        String zipPath = zipUrl.getPath();

        File temp = File.createTempFile("GsZipTest", ".tmp");
        String dirPath = temp.getAbsolutePath() + ".d/";

        assertTrue(GsZip.unpackToFolder(zipPath, dirPath, "geansea"));

        fileCheck(dirPath + "file_0.txt", 0, 0);
        fileCheck(dirPath + "file_1.txt", 256, 0xe42ddc29);
        folderCheck(dirPath + "empty");
        folderCheck(dirPath + "sub");
        fileCheck(dirPath + "sub/file_2.txt", 256, 0x996bcc1b);
    }

    @Test
    public void pack_folder() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL dirUrl = classLoader.getResource("folder");
        String dirPath = dirUrl.getPath();

        File zip = File.createTempFile("GsZipTest", ".tmp");
        String zipPath = zip.getAbsolutePath() + ".zip";

        assertTrue(GsZip.packFolder(dirPath, zipPath, "", false));

        GsZipFileTest.openCheck(zipPath, "");
    }

    @Test
    public void pack_folder_enc() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL dirUrl = classLoader.getResource("folder");
        String dirPath = dirUrl.getPath();

        File zip = File.createTempFile("GsZipTest", ".tmp");
        String zipPath = zip.getAbsolutePath() + ".zip";

        assertTrue(GsZip.packFolder(dirPath, zipPath, "geansea", true));

        GsZipFile zipFile = GsZipFile.create(zipPath);
        assertEquals(6, zipFile.size());
        assertTrue(zipFile.needPassword());
        zipFile.setPassword("geansea");

        GsZipFileTest.folderEntryCheck(zipFile, "folder");
        GsZipFileTest.fileEntryCheck(zipFile, "folder/file_0.txt", 0);
        GsZipFileTest.fileEntryCheck(zipFile, "folder/file_1.txt", 256);
        GsZipFileTest.folderEntryCheck(zipFile, "folder/empty");
        GsZipFileTest.folderEntryCheck(zipFile, "folder/sub");
        GsZipFileTest.fileEntryCheck(zipFile, "folder/sub/file_2.txt", 256);
    }

    @Test
    public void pack_file() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("folder/file_1.txt");
        String filePath = fileUrl.getPath();

        File zip = File.createTempFile("GsZipTest", ".tmp");
        String zipPath = zip.getAbsolutePath() + ".zip";

        assertTrue(GsZip.packFile(filePath, zipPath, ""));

        GsZipFile zipFile = GsZipFile.create(zipPath);
        assertNotNull(zipFile);
        assertEquals(1, zipFile.size());
        GsZipFileTest.fileEntryCheck(zipFile, "file_1.txt", 256);
    }

    @Test
    public void pack_file_enc() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("folder/file_1.txt");
        String filePath = fileUrl.getPath();

        File zip = File.createTempFile("GsZipTest", ".tmp");
        String zipPath = zip.getAbsolutePath() + ".zip";

        assertTrue(GsZip.packFile(filePath, zipPath, "geansea"));

        GsZipFile zipFile = GsZipFile.create(zipPath);
        assertEquals(1, zipFile.size());
        assertTrue(zipFile.needPassword());
        zipFile.setPassword("geansea");
        GsZipFileTest.fileEntryCheck(zipFile, "file_1.txt", 256);
    }

    @Test
    public void pack_file_error() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("folder/file_1.txt");
        String filePath = fileUrl.getPath();

        File zip = File.createTempFile("GsZipTest", ".tmp");
        String zipPath = zip.getAbsolutePath() + ".zip";

        assertFalse(GsZip.packFolder(filePath, zipPath, "", false));
    }
}
