package org.embeddedt.vintagefix.mixin.predicates;

import com.google.common.base.Predicate;
import org.embeddedt.vintagefix.impl.Deduplicator;
import org.embeddedt.vintagefix.util.PredicateHelper;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.multipart.ConditionAnd;
import net.minecraft.client.renderer.block.model.multipart.ICondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConditionAnd.class, priority = 2000)
public class AndConditionMixin {
    @Shadow
    @Final
    private Iterable<? extends ICondition> conditions;

    /**
     * @reason Use cached result predicates
     * @author malte0811
     */
    @Overwrite
    public Predicate<IBlockState> getPredicate(BlockStateContainer stateContainer) {
        return Deduplicator.and(PredicateHelper.toCanonicalList(conditions, stateContainer));
    }
}
