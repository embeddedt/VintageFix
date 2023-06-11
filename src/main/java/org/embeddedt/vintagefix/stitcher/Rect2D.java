package org.embeddedt.vintagefix.stitcher;

public class Rect2D implements Comparable<Rect2D> {
    public int x;
    public int y;
    public int width;
    public int height;

    public Rect2D() { }

    public Rect2D(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public int compareTo(Rect2D o) {
        int ourArea = width * height;
        int theirArea = o.width * o.height;
        // will make sure that larger areas go first
        return theirArea - ourArea;
    }
}
