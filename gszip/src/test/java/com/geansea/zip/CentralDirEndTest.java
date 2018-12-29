package com.geansea.zip;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CentralDirEndTest {
    private static final byte[] bytes = new byte[]{
            'P', 'K', 0x05, 0x06,
            0x00, 0x00,
            0x00, 0x00,
            0x12, 0x34,
            0x12, 0x34,
            0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x08,
            0x09, 0x00,
            'a', 'b', 'c',
            (byte) 0xE4, (byte) 0xB8, (byte) 0xAD,
            (byte) 0xE6, (byte) 0x96, (byte) 0x87,
    };
    private static final int entryCount = 0x3412;
    private static final long dirSize = 0x04030201;
    private static final long dirOffset = 0x08070605;
    private static final String comment = "abc\u4E2D\u6587";

    @Test
    public void testRead() throws Exception {
        CentralDirEnd end = new CentralDirEnd();
        end.readFrom(bytes);
        assertEquals(entryCount, end.getEntryCount());
        assertEquals(dirOffset, end.getDirOffset());
        assertEquals(dirSize, end.getDirSize());
        assertEquals(comment, end.getComment(StandardCharsets.UTF_8));
    }

    @Test
    public void testRead_more() throws Exception {
        byte[] in = Arrays.copyOf(bytes, bytes.length + 1);
        CentralDirEnd end = new CentralDirEnd();
        end.readFrom(in);
        assertEquals(entryCount, end.getEntryCount());
        assertEquals(dirOffset, end.getDirOffset());
        assertEquals(dirSize, end.getDirSize());
        assertEquals(comment, end.getComment(StandardCharsets.UTF_8));
    }

    @Test
    public void testRead_less() {
        byte[] in = Arrays.copyOf(bytes, bytes.length - 1);
        CentralDirEnd end = new CentralDirEnd();
        try {
            end.readFrom(in);
            fail("A GsZipException should be thrown");
        } catch (GsZipException ignored) {
        }
    }

    @Test
    public void testWrite() throws Exception {
        byte[] out = new byte[bytes.length];
        CentralDirEnd end = new CentralDirEnd();
        end.setEntryCount(entryCount);
        end.setDirRange(dirOffset, dirSize);
        end.setComment(comment, StandardCharsets.UTF_8);
        end.writeTo(out);
        assertArrayEquals(bytes, out);
    }

    @Test
    public void testWrite_less() {
        byte[] out = new byte[bytes.length - 1];
        CentralDirEnd end = new CentralDirEnd();
        end.setEntryCount(entryCount);
        end.setDirRange(dirOffset, dirSize);
        end.setComment(comment, StandardCharsets.UTF_8);
        try {
            end.writeTo(out);
            fail("A GsZipException should be thrown");
        } catch (GsZipException ignored) {
        }
    }
}
