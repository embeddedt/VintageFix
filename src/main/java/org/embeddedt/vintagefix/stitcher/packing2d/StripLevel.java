package org.embeddedt.vintagefix.stitcher.packing2d;

import org.embeddedt.vintagefix.stitcher.Rect2D;

class StripLevel {
    private final int width;
    private final int top;
    private int availableWidth;
    private int tallest = -1;

    StripLevel(int width, int top) {
        this.width = width;
        this.availableWidth = width;
        this.top = top;
    }

    boolean checkFitRectangle(Rect2D r) {
        return (tallest < 0 || r.height <= tallest) && r.width <= availableWidth;
    }

    StripLevel fitRectangle(Rect2D r) {
        if (tallest >= 0 && r.height > tallest) {
            return null;
        }
        StripLevel newStrip = null;
        int leftOver = availableWidth - r.width;
        if (leftOver >= 0) {
            r.x = width - availableWidth;
            r.y = top;
            if (tallest == -1) {
                tallest = r.height;
            }
            if (r.height < tallest) {
                newStrip = new StripLevel(width, top + r.height);
                newStrip.availableWidth = availableWidth;
                newStrip.tallest = tallest - r.height;
                tallest = r.height;
            }
            availableWidth = leftOver;
        }
        return newStrip;
    }

    int availableWidth() {
        return availableWidth;
    }

    boolean canFit(Rect2D r) {
        return availableWidth - r.width >= 0 && (tallest < 0 || r.height <= tallest);
    }
}
