package com.geansea.zip;

import org.checkerframework.checker.index.qual.GTENegativeOne;

import java.io.InputStream;

/**
 * Sub-class of InputStream to add some useful methods.
 */
public abstract class GsZipInputStream extends InputStream {
    /**
     * Get the length of stream.
     * @return the length, -1 if unknown
     */
    public @GTENegativeOne long getLength() {
        return -1;
    }

    /**
     * Reset the stream to the start.
     * @throws GsZipException if not supporteds
     */
    public void restart() throws GsZipException {
        throw new GsZipException("Restart is not supported");
    }
}
