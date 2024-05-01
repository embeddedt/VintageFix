package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.util.EnumFacing;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;

@Mixin(targets = {"net/minecraftforge/client/model/ItemLayerModel$FaceData"}, remap = false)
@ClientOnlyMixin
public class FaceDataMixin {
    private int[] faceDataStore;
    private int faceDataSize;

    @Shadow
    @Final
    private int vMax;


    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/EnumMap;put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object skipPut(EnumMap<?, ?> map, Enum<?> key, Object object) {
        return null;
    }

    private static int getOrdinal(EnumFacing facing) {
        switch(facing) {
            case WEST:
                return 0;
            case EAST:
                return 1;
            case UP:
                return 2;
            case DOWN:
                return 3;
            default:
                throw new IllegalArgumentException("Unexpected facing");
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initBacking(int uMax, int vMax, CallbackInfo ci) {
        this.faceDataSize = ((uMax * vMax + 31) / 32);
        this.faceDataStore = new int[4 * this.faceDataSize];
    }

    /**
     * @author embeddedt
     * @reason use our backing store
     */
    @Overwrite
    public void set(EnumFacing facing, int u, int v) {
        int idx = v * vMax + u;
        this.faceDataStore[getOrdinal(facing) * faceDataSize + (idx >> 5)] |= 1 << (idx & 31);
    }

    /**
     * @author embeddedt
     * @reason use our backing store
     */
    @Overwrite
    public boolean get(EnumFacing facing, int u, int v) {
        int idx = v * vMax + u;
        return (this.faceDataStore[getOrdinal(facing) * faceDataSize + (idx >> 5)] & (1 << (idx & 31))) != 0;
    }
}
