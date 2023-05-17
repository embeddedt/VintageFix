package org.embeddedt.vintagefix.stitcher.packing2d;

import org.embeddedt.vintagefix.stitcher.Rect2D;

import java.util.ArrayList;
import java.util.List;

public class PackerBFDH<T extends Rect2D> extends Packer<T> {
    private final List<StripLevel> levels;

    public PackerBFDH(int stripWidth, List<T> rectangles) {
        super(stripWidth, rectangles);
        levels = new ArrayList<>();
    }

    @Override
    public List<T> pack() {
        int top = 0;
        sortByNonIncreasingHeight(rectangles);
        for (T r : rectangles) {
            StripLevel levelWithSmallestResidual = null;
            for (StripLevel level : levels) {
                if (!level.canFit(r)) {
                    continue;
                }
                if (levelWithSmallestResidual != null &&
                    levelWithSmallestResidual.availableWidth() > level.availableWidth()) {
                    levelWithSmallestResidual = level;
                } else if (levelWithSmallestResidual == null) {
                    levelWithSmallestResidual = level;
                }
            }
            if (levelWithSmallestResidual == null) {
                StripLevel level = new StripLevel(stripWidth, top);
                level.fitRectangle(r);
                levels.add(level);
                top += r.height;
            } else {
                StripLevel newLevel = levelWithSmallestResidual.fitRectangle(r);
                if (newLevel != null) {
                    levels.add(levels.indexOf(levelWithSmallestResidual) + 1, newLevel);
                }
            }

        }
        return rectangles;
    }
}
