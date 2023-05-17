package org.embeddedt.vintagefix.stitcher;

public class WrongDimensionException extends RuntimeException {
    public static WrongDimensionException INSTANCE = new WrongDimensionException();
}
