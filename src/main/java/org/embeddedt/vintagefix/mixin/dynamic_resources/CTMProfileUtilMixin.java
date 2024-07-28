package org.embeddedt.vintagefix.mixin.dynamic_resources;

import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import team.chisel.ctm.client.util.ProfileUtil;

import javax.annotation.Nonnull;

/**
 * The thread-local is insanely slow. We overwrite it and never do profiling. Use a Java profiler if you
 * want to measure CTM impact.
 */
@Mixin(value = ProfileUtil.class, remap = false)
@LateMixin
@ClientOnlyMixin
public class CTMProfileUtilMixin {
    /**
     * @author embeddedt
     * @reason See above
     */
    @Overwrite
    public static void start(@Nonnull String section) {

    }

    /**
     * @author embeddedt
     * @reason The thread-local is insanely slow. Overwrite it and never do profiling. Use a Java profiler if you
     * want to measure CTM impact.
     */
    @Overwrite
    public static void end() {

    }

    /**
     * @author embeddedt
     * @reason The thread-local is insanely slow. Overwrite it and never do profiling. Use a Java profiler if you
     * want to measure CTM impact.
     */
    @Overwrite
    public static void endAndStart(@Nonnull String section) {

    }
}
