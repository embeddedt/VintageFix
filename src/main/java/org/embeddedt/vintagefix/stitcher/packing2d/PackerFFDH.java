package org.embeddedt.vintagefix.stitcher.packing2d;

import org.embeddedt.vintagefix.stitcher.Rect2D;

import java.util.ArrayList;
import java.util.List;

class PackerFFDH<T extends Rect2D> extends Packer<T> {
    private final List<StripLevel> levels = new ArrayList<>(1);
    private int top = 0;

    public PackerFFDH(int stripWidth, List<T> rectangles) {
        super(stripWidth, rectangles);
    }

    @Override
    public List<T> pack() {
        sortByNonIncreasingHeight(rectangles);
        for (T r : rectangles) {
            boolean fitsOnALevel = false;
            for (int i = 0; i < levels.size(); i++) {
                StripLevel level = levels.get(i);
                fitsOnALevel = level.checkFitRectangle(r);
                if (!fitsOnALevel) {
                    continue;
                }
                StripLevel newStrip = level.fitRectangle(r);
                if (newStrip != null) {
                    levels.add(0, newStrip);
                }
                break;
            }
            if (fitsOnALevel) {
                continue;
            }
            StripLevel level = new StripLevel(stripWidth, top);
            level.fitRectangle(r);
            levels.add(level);
            top += r.height;
        }
        return rectangles;
    }
}
