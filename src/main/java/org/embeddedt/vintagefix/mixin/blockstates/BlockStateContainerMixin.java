package org.embeddedt.vintagefix.mixin.blockstates;

import com.google.common.collect.ImmutableSortedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import org.embeddedt.vintagefix.fastmap.FakeStateMap;
import org.embeddedt.vintagefix.impl.StateHolderImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(BlockStateContainer.class)
public class BlockStateContainerMixin {
    @Shadow
    @Final
    private ImmutableSortedMap<String, IProperty<?>> properties;

    /**
     * Use a fastutil map for String->Property.
     */
    @ModifyVariable(method = "<init>(Lnet/minecraft/block/Block;[Lnet/minecraft/block/properties/IProperty;Lcom/google/common/collect/ImmutableMap;)V", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"), ordinal = 0, index = 4)
    private Map<String, IProperty<?>> useFastUtilMap(Map<String, IProperty<?>> map) {
        return new Object2ObjectOpenHashMap<>(map);
    }

    /**
     * Replaces map2 (the map of property-value pairs to state objects) with a simpler variant that can be inserted
     * into quickly. This is because the replacement blockstate logic doesn't need most of the features of a map.
     */
    @ModifyVariable(method = "<init>(Lnet/minecraft/block/Block;[Lnet/minecraft/block/properties/IProperty;Lcom/google/common/collect/ImmutableMap;)V", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/Maps;newLinkedHashMap()Ljava/util/LinkedHashMap;"), ordinal = 1, index = 5)
    private Map<Map<IProperty<?>, Comparable<?>>, BlockStateContainer.StateImplementation> useArrayMap(Map<Map<IProperty<?>, Comparable<?>>, BlockStateContainer.StateImplementation> in) {
        int numStates = 1;
        for(IProperty<?> prop : this.properties.values()) {
            numStates *= prop.getAllowedValues().size();
        }
        return new FakeStateMap<>(numStates);
    }

    @Inject(method = "<init>(Lnet/minecraft/block/Block;[Lnet/minecraft/block/properties/IProperty;Lcom/google/common/collect/ImmutableMap;)V", at = @At("RETURN"))
    private void clearStateWhenFinished(CallbackInfo ci) {
        StateHolderImpl.clearCachedStateMaps();
    }
}
