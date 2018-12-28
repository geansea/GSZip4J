package com.geansea.zip;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class CentralDirEndTest {
    private static final byte[] bytes = new byte[]{
            'P', 'K', 0x05, 0x06,
            0, 0,
            0, 0,
            0x12, 0x34,
            0x12, 0x34,
            0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x08,
            0x09, 0,
            'a', 'b', 'c',
            (byte) 0xE4, (byte) 0xB8, (byte) 0xAD,
            (byte) 0xE6, (byte) 0x96, (byte) 0x87,
    };
    private static final int entryCount = 0x3412;
    private static final long dirSize = 0x04030201;
    private static final long dirOffset = 0x08070605;
    private static final String comment = "abc\u4E2D\u6587";

    @Test
    public void read() throws Exception {
        CentralDirEnd end = new CentralDirEnd();
        end.readFrom(bytes);
        assertEquals(entryCount, end.getEntryCount());
        assertEquals(dirOffset, end.getDirOffset());
        assertEquals(dirSize, end.getDirSize());
        assertEquals(comment, end.getComment(StandardCharsets.UTF_8));
    }
}
