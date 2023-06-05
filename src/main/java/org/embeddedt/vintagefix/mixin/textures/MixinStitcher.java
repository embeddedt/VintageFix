package org.embeddedt.vintagefix.mixin.textures;

import net.minecraftforge.fml.common.ProgressManager;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.stitcher.TooBigException;
import org.embeddedt.vintagefix.stitcher.TurboStitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.StitcherException;
import net.minecraft.client.renderer.texture.Stitcher;

import java.util.List;
import java.util.Set;

@Mixin(Stitcher.class)
@ClientOnlyMixin
public abstract class MixinStitcher {
    @Shadow
    @Final
    private List<Stitcher.Slot> stitchSlots;

    @Shadow
    private int currentHeight;
    @Shadow
    private int currentWidth;
    private TurboStitcher masterStitcher;

    @Inject(method = "<init>",
        at = @At(value = "RETURN"),
        require = 1)
    private void initTurbo(int maxWidth, int maxHeight, int maxTileDimension, int mipmapLevelStitcher, CallbackInfo ci) {
        masterStitcher = new TurboStitcher(maxWidth, maxHeight, true);
    }

    @Redirect(method = "addSprite",
        at = @At(value = "INVOKE",
            target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"),
        require = 1)
    private boolean hijackAdd(Set<Stitcher.Holder> instance, Object e) {
        masterStitcher.addSprite((Stitcher.Holder) e);
        return true;
    }

    @Inject(method = "doStitch",
        at = @At(value = "HEAD"),
        cancellable = true,
        require = 1)
    private void doTurboStitch(CallbackInfo ci) {
        ci.cancel();
        try {
            ProgressManager.ProgressBar bar = ProgressManager.push("Texture stitching", 2);
            bar.step("Stitching master atlas");
            masterStitcher.stitch();
            currentWidth = masterStitcher.width;
            currentHeight = masterStitcher.height;
            bar.step("Extracting stitched textures");
            stitchSlots.clear();
            stitchSlots.addAll(masterStitcher.getSlots());
            ProgressManager.pop(bar);
        } catch (TooBigException ignored) {
            throw new StitcherException(null,
                "Unable to fit all textures into atlas. Maybe try a lower resolution resourcepack?");
        } finally {
            masterStitcher.reset();
        }
    }
}
