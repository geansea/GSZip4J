package com.geansea.zip;

import android.support.annotation.NonNull;

final class PKWareKey {
    private static final int KEY0 = 0x12345678;
    private static final int KEY1 = 0x23456789;
    private static final int KEY2 = 0x34567890;
    private static final int UPDATE = 134775813;
    private static final int CRC_POLY = 0xEDB88320;
    @NonNull
    private static final int[] CRC_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; ++i) {
            int r = i;
            for (int j = 0; j < 8; ++j) {
                if ((r & 1) == 1) {
                    r = (r >>> 1) ^ CRC_POLY;
                } else {
                    r >>>= 1;
                }
            }
            CRC_TABLE[i] = r;
        }
    }

    private int key0;
    private int key1;
    private int key2;

    PKWareKey() {
        reset();
    }

    void reset() {
        key0 = KEY0;
        key1 = KEY1;
        key2 = KEY2;
    }

    void update(byte c) {
        key0 = crc32(key0, c);
        key1 = (key1 + (key0 & 0xFF)) * UPDATE + 1;
        key2 = crc32(key2, (byte) (key1 >>> 24));
    }

    byte cryptByte() {
        int temp = key2 | 2;
        return (byte) ((temp * (temp ^ 1)) >>> 8);
    }

    private static int crc32(int oldCrc, byte c) {
        return ((oldCrc >>> 8) ^ CRC_TABLE[(oldCrc ^ c) & 0xFF]);
    }
}
