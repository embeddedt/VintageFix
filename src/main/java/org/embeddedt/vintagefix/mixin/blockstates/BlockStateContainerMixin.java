package org.embeddedt.vintagefix.mixin.blockstates;

import com.google.common.collect.ImmutableSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import org.embeddedt.vintagefix.fastmap.FakeStateMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@Mixin(BlockStateContainer.class)
public class BlockStateContainerMixin {
    @Shadow
    @Final
    private ImmutableSortedMap<String, IProperty<?>> properties;

    @ModifyVariable(method = "<init>(Lnet/minecraft/block/Block;[Lnet/minecraft/block/properties/IProperty;Lcom/google/common/collect/ImmutableMap;)V", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"), ordinal = 0, index = 4)
    private Map<String, IProperty<?>> useFastUtilMap(Map<String, IProperty<?>> map) {
        return new Object2ObjectOpenHashMap<>(map);
    }

    @ModifyVariable(method = "<init>(Lnet/minecraft/block/Block;[Lnet/minecraft/block/properties/IProperty;Lcom/google/common/collect/ImmutableMap;)V", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/Maps;newLinkedHashMap()Ljava/util/LinkedHashMap;"), ordinal = 1, index = 5)
    private Map<Map<IProperty<?>, Comparable<?>>, BlockStateContainer.StateImplementation> useArrayMap(Map<Map<IProperty<?>, Comparable<?>>, BlockStateContainer.StateImplementation> in) {
        int numStates = 1;
        for(IProperty<?> prop : this.properties.values()) {
            numStates *= prop.getAllowedValues().size();
        }
        return new FakeStateMap<>(numStates);
    }
}
