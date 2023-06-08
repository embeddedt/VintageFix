package org.embeddedt.vintagefix.util;

import java.io.FileNotFoundException;

public class FastFileNotFoundException extends FileNotFoundException {
    public FastFileNotFoundException(String message) {
        super(message);
    }

    /**
     * Make this exception fast to throw.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
