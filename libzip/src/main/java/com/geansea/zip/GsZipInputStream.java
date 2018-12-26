package com.geansea.zip;

import java.io.IOException;
import java.io.InputStream;

/**
 * Sub-class of InputStream to add some useful methods.
 */
public class GsZipInputStream extends InputStream {
    @Override
    public int read() throws IOException {
        return -1;
    }

    /**
     * Reset the stream to the start.
     * @throws IOException if not supported or
     */
    public void restart() throws IOException {
        throw new IOException("Restart is not supported");
    }
}
