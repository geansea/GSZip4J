package com.geansea.zip;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import static org.junit.Assert.*;

public class GsZipPackerTest {
    @Test
    public void pack() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL dirUrl = classLoader.getResource("folder");
        String baseDir = dirUrl.getFile() + "/";

        GsZipPacker packer = new GsZipPacker();
        assertTrue(packer.addFile("file_0.txt", baseDir + "file_0.txt"));
        assertTrue(packer.addFile("file_1.txt", baseDir + "file_1.txt"));
        assertTrue(packer.addFolder("empty"));
        assertTrue(packer.addFolder("sub/"));
        assertTrue(packer.addFile("sub/file_2.txt", baseDir + "sub/file_2.txt"));

        File zip = File.createTempFile("GsZipPackerTest", ".tmp.zip");
        FileOutputStream zipStream = new FileOutputStream(zip);
        assertTrue(packer.packTo(zipStream, ""));
        GsZipFileTest.openCheck(zip.getAbsolutePath(), "");
    }

    @Test
    public void pack_enc() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL dirUrl = classLoader.getResource("folder");
        String baseDir = dirUrl.getFile() + "/";

        GsZipPacker packer = new GsZipPacker();
        assertTrue(packer.addFile("file_0.txt", baseDir + "file_0.txt"));
        assertTrue(packer.addFile("file_1.txt", baseDir + "file_1.txt"));
        assertTrue(packer.addFolder("empty"));
        assertTrue(packer.addFolder("sub/"));
        assertTrue(packer.addFile("sub/file_2.txt", baseDir + "sub/file_2.txt"));

        File zip = File.createTempFile("GsZipPackerTest", ".tmp");
        String zipPath = zip.getAbsolutePath() + ".zip";
        assertTrue(packer.packTo(zipPath, "geansea"));
        GsZipFileTest.openCheck(zipPath, "geansea");
    }

    @Test
    public void pack_empty() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL dirUrl = classLoader.getResource("folder");
        String baseDir = dirUrl.getFile() + "/";

        GsZipPacker packer = new GsZipPacker();
        packer.setComment("test comment");

        File zip = File.createTempFile("GsZipPackerTest", ".tmp.zip");
        FileOutputStream zipStream = new FileOutputStream(zip);
        assertTrue(packer.packTo(zipStream, ""));

        GsZipFile zipFile = GsZipFile.create(zip.getAbsolutePath());
        assertNotNull(zipFile);
        assertEquals(0, zipFile.size());
        assertTrue(zipFile.getComment().equals("test comment"));
    }
}
