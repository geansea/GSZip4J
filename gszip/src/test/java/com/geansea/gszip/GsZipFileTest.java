package com.geansea.gszip;

import android.support.annotation.NonNull;

import com.geansea.gszip.util.GsZipUtil;

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
        assertNull(stream);
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
        GsZipFile zip = GsZipFile.create(path);
        assertNotNull(zip);
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
        GsZipFile zip = GsZipFile.create(fileUrl.getFile());
        assertNotNull(zip);
        assertEquals(9, zip.size());
        assertFalse(zip.needPassword());

        folderEntryCheck(zip, "Images/");
        folderEntryCheck(zip, "Images/目录一");
        folderEntryCheck(zip, "Images/目录二");
        fileEntryCheck(zip, "Images/目录一/第零张.jpg", 15201);
        fileEntryCheck(zip, "Images/目录一/第十张.jpg", 13454);
        fileEntryCheck(zip, "Images/目录一/第二十张.jpg", 18996);
        fileEntryCheck(zip, "Images/目录二/第五张.jpg", 12063);
        fileEntryCheck(zip, "Images/目录二/第五十五张.jpg", 20466);
        fileEntryCheck(zip, "Images/目录二/第五百五十五张.jpg", 14546);

        GsZipEntryNode tree = zip.getEntryTree();
        assertNotNull(tree.getChildWithPath("Images"));
        assertNotNull(tree.getChildWithPath("Images/目录一//"));
        assertNotNull(tree.getChildWithPath("Images//目录二"));
        assertNotNull(tree.getChildWithPath("Images/目录一/第零张.jpg"));
        assertNotNull(tree.getChildWithPath("Images/目录一/第十张.jpg"));
        assertNotNull(tree.getChildWithPath("Images/目录一/第二十张.jpg"));

        GsZipEntryNode node = tree.getChildWithPath("Images/目录二");
        assertNotNull(node);
        assertNotNull(node.getChild("第五张.jpg"));
        assertNotNull(node.getChild("第五十五张.jpg"));
        assertNotNull(node.getChild("第五百五十五张.jpg"));
    }
}
