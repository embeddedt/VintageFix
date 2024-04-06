package org.embeddedt.vintagefix.util;

import javax.vecmath.Matrix4f;

public class MatrixHelper {
    public static final Matrix4f IDENTITY_4X4;

    static {
        IDENTITY_4X4 = new Matrix4f();
        IDENTITY_4X4.setIdentity();
    }
}
