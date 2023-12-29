package org.embeddedt.vintagefix.mixin.dynamic_resources;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;

@LateMixin
@ClientOnlyMixin
@Mixin(VoxelBlob.class)
public class VoxelBlobMixin {
    @Shadow(remap = false)
    @Final
    private static int array_size;
    @Shadow(remap = false)
    @Final
    private int[] values;
    private static Map<BlockRenderLayer, Int2BooleanOpenHashMap> vfix$layerFilters;

    private static void initLayerFilters() {
        vfix$layerFilters = new EnumMap<>(BlockRenderLayer.class);
        for(BlockRenderLayer layer : BlockRenderLayer.values()) {
            vfix$layerFilters.put(layer, new Int2BooleanOpenHashMap());
        }
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void setupLayerCache(CallbackInfo ci) {
        initLayerFilters();
    }

    @Inject(method = "updateCacheClient", at = @At("HEAD"), cancellable = true, remap = false)
    private static void clearLayerCache(CallbackInfo ci) {
        ci.cancel();
        synchronized (VoxelBlob.class) {
            if(vfix$layerFilters == null)
                initLayerFilters();
            else
                vfix$layerFilters.values().forEach(Int2BooleanOpenHashMap::clear);
        }
    }

    /**
     * @author embeddedt
     * @reason rewrite to use dynamic cache
     */
    @Overwrite(remap = false)
    public boolean filter(BlockRenderLayer layer) {
        Int2BooleanOpenHashMap layerFilterMap = vfix$layerFilters.get(layer);
        boolean hasValues = false;
        for(int i = 0; i < array_size; i++) {
            int blockId = this.values[i];
            if(blockId != 0) {
                boolean isInLayer = false;
                synchronized (layerFilterMap) {
                    if(!layerFilterMap.containsKey(blockId)) {
                        Block block = ModUtil.getStateById(blockId).getBlock();
                        for(IBlockState state : block.getBlockState().getValidStates()) {
                            if(state.getBlock() != block)
                                continue;
                            isInLayer = block.canRenderInLayer(state, layer);
                            if(isInLayer)
                                break;
                        }
                        layerFilterMap.put(blockId, isInLayer);
                    } else
                        isInLayer = layerFilterMap.get(blockId);
                }
                if(isInLayer) {
                    hasValues = true;
                } else
                    this.values[i] = 0;
            }
        }
        return hasValues;
    }
}
