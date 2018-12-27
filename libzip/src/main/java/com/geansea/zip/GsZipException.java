package com.geansea.zip;

import org.checkerframework.checker.nullness.qual.NonNull;

public class GsZipException extends Exception {
    public GsZipException(@NonNull String message) {
        super(message);
    }
}
