package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class StateMapperHandler {
    private final LoadingCache<Block, Map<IBlockState, ModelResourceLocation>> modelLocationCache;

    public StateMapperHandler(BlockStateMapper blockStateMapper) {
        this.modelLocationCache = CacheBuilder.newBuilder()
            .maximumWeight(100000)
            .weigher((Weigher<Block, Map<IBlockState, ModelResourceLocation>>) (key, value) -> value.size())
            .build(new CacheLoader<Block, Map<IBlockState, ModelResourceLocation>>() {
                @Override
                public Map<IBlockState, ModelResourceLocation> load(Block key) throws Exception {
                    synchronized (blockStateMapper) {
                        return ImmutableMap.copyOf(blockStateMapper.getVariants(key));
                    }
                }
            });
    }

    public ModelResourceLocation getModelLocationForState(IBlockState state) {
        try {
            return modelLocationCache.get(state.getBlock()).get(state);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
