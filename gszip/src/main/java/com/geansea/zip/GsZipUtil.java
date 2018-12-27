package com.geansea.zip;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Stack;
import java.util.zip.CRC32;

/**
 * Util class for GsZip.
 */
final class GsZipUtil {
    static final int BUFFER_SIZE = 1024;

    /**
     * Check the state and throw exception if not true
     *
     * @param state        the state to check
     * @param errorMessage the error message for exception
     * @throws GsZipException if state is false
     */
    static void check(boolean state, @NonNull String errorMessage) throws GsZipException {
        if (!state) {
            throw new GsZipException(errorMessage);
        }
    }

    /**
     * Calculate the CRC32 of stream.
     *
     * @param stream the stream to calculate
     * @return the CRC32 of stream
     * @throws IOException if throws
     */
    static int calcStreamCRC(@NonNull GsZipInputStream stream) throws IOException {
        CRC32 crc32 = new CRC32();
        byte[] buffer = new byte[BUFFER_SIZE];
        stream.restart();
        int count;
        while ((count = stream.read(buffer)) > 0) {
            crc32.update(buffer, 0, count);
        }
        return (int) crc32.getValue();
    }

    /**
     * Calculate the length of stream.
     *
     * @param stream the stream to calculate
     * @return the length of stream
     * @throws IOException if throws
     */
    static int calcStreamLength(@NonNull GsZipInputStream stream) throws IOException {
        long length = 0;
        stream.restart();
        long count;
        while ((count = stream.skip(BUFFER_SIZE)) > 0) {
            length += count;
        }
        return (int) length;
    }

    /**
     * Normalize the path.
     *
     * @param path the path to normalize
     * @return normalized path
     */
    static @NonNull String normalizePath(@NonNull String path) {
        Stack<String> parts = new Stack<>();
        for (String part : path.split("[/\\\\]")) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!parts.isEmpty()) {
                    parts.pop();
                    continue;
                }
            }
            parts.push(part);
        }
        return String.join("/", parts);
    }

    static @NonNull String getCanonicalPath(@NonNull String path) {
        Stack<String> parts = new Stack<>();
        for (String part : path.split("[/\\\\]")) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!parts.isEmpty()) {
                    parts.pop();
                    continue;
                }
            }
            parts.push(part);
        }
        return String.join("/", parts);
    }

    public static @NonNull String getParentPath(@NonNull String path) {
        int parentPos = path.indexOf("/");
        if (parentPos < 0) {
            return "";
        }
        return path.substring(0, parentPos);
    }
}
