package com.geansea.zip;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

public class GsZipFileTest {
    static void folderEntryCheck(@NonNull GsZipFile zip, @NonNull String name) throws Exception {
        GsZipEntry entry = zip.getEntry(name);
        assertNotNull(entry);
        assertFalse(entry.isFile());
        assertEquals(0, entry.getOriginalSize());

        InputStream stream = zip.getInputStream(entry.getIndex());
        //assertNull(stream);
    }

    static void fileEntryCheck(@NonNull GsZipFile zip, @NonNull String name, int size) throws Exception {
        GsZipEntry entry = zip.getEntry(name);
        assertNotNull(entry);
        assertTrue(entry.isFile());
        assertEquals(size, entry.getOriginalSize());
        if (entry.getCompressedSize() < size) {
            assertTrue(entry.isCompressed());
        }

        InputStream stream = zip.getInputStream(name);
        assertNotNull(stream);
        stream = new BufferedInputStream(stream);
        stream.mark(Integer.MAX_VALUE);
        assertEquals(size, GsZipUtil.getStreamLength(stream));
        assertEquals(entry.getCRC(), GsZipUtil.getStreamCRC(stream));
    }

    static void openCheck(@NonNull String path, @NonNull String password) throws Exception {
        GsZipFile zip = new GsZipFile(path);
        assertEquals(5, zip.size());

        if (password.isEmpty()) {
            assertFalse(zip.needPassword());
        } else {
            assertTrue(zip.needPassword());
            zip.setPassword(password);
        }

        fileEntryCheck(zip, "file_0.txt", 0);
        fileEntryCheck(zip, "file_1.txt", 256);
        folderEntryCheck(zip, "empty");
        folderEntryCheck(zip, "sub");
        fileEntryCheck(zip, "sub/file_2.txt", 256);

        GsZipEntryNode tree = zip.getEntryTree();
        assertNotNull(tree.getChild("file_0.txt"));
        assertNotNull(tree.getChild("file_1.txt"));
        assertNotNull(tree.getChild("empty"));
        assertNotNull(tree.getChildWithPath("sub"));
        assertNotNull(tree.getChildWithPath("sub/file_2.txt"));
        assertNull(tree.getChild("empty1"));
        assertNull(tree.getChild("sub/file_2.txt"));
    }

    @Test
    public void open_store() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("store.zip");
        openCheck(fileUrl.getFile(), "");
    }

    @Test
    public void open_store_encrypt() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("store_enc.zip");
        openCheck(fileUrl.getFile(), "geansea");
    }

    @Test
    public void open_flate() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("flate.zip");
        openCheck(fileUrl.getFile(), "");
    }

    @Test
    public void open_flate_encrypt() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("flate_enc.zip");
        openCheck(fileUrl.getFile(), "geansea");
    }

    @Test
    public void open_cjk() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("cjk_winrar.zip");
        GsZipFile zip = new GsZipFile(fileUrl.getFile());
        assertEquals(9, zip.size());
        assertFalse(zip.needPassword());

        folderEntryCheck(zip, "Images/");
        folderEntryCheck(zip, "Images/\u76ee\u5f55\u4e00");
        folderEntryCheck(zip, "Images/\u76ee\u5f55\u4e8c");
        fileEntryCheck(zip, "Images/\u76ee\u5f55\u4e00/\u7b2c\u96f6\u5f20.jpg", 15201);
        fileEntryCheck(zip, "Images/\u76ee\u5f55\u4e00/\u7b2c\u5341\u5f20.jpg", 13454);
        fileEntryCheck(zip, "Images/\u76ee\u5f55\u4e00/\u7b2c\u4e8c\u5341\u5f20.jpg", 18996);
        fileEntryCheck(zip, "Images/\u76ee\u5f55\u4e8c/\u7b2c\u4e94\u5f20.jpg", 12063);
        fileEntryCheck(zip, "Images/\u76ee\u5f55\u4e8c/\u7b2c\u4e94\u5341\u4e94\u5f20.jpg", 20466);
        fileEntryCheck(zip, "Images/\u76ee\u5f55\u4e8c/\u7b2c\u4e94\u767e\u4e94\u5341\u4e94\u5f20.jpg", 14546);

        GsZipEntryNode tree = zip.getEntryTree();
        assertNotNull(tree.getChildWithPath("Images"));
        assertNotNull(tree.getChildWithPath("Images/\u76ee\u5f55\u4e00//"));
        assertNotNull(tree.getChildWithPath("Images//\u76ee\u5f55\u4e8c"));
        assertNotNull(tree.getChildWithPath("Images/\u76ee\u5f55\u4e00/\u7b2c\u96f6\u5f20.jpg"));
        assertNotNull(tree.getChildWithPath("Images/\u76ee\u5f55\u4e00/\u7b2c\u5341\u5f20.jpg"));
        assertNotNull(tree.getChildWithPath("Images/\u76ee\u5f55\u4e00/\u7b2c\u4e8c\u5341\u5f20.jpg"));

        GsZipEntryNode node = tree.getChildWithPath("Images/\u76ee\u5f55\u4e8c");
        assertNotNull(node);
        assertNotNull(node.getChild("\u7b2c\u4e94\u5f20.jpg"));
        assertNotNull(node.getChild("\u7b2c\u4e94\u5341\u4e94\u5f20.jpg"));
        assertNotNull(node.getChild("\u7b2c\u4e94\u767e\u4e94\u5341\u4e94\u5f20.jpg"));
    }
}
