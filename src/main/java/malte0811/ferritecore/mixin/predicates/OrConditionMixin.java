package malte0811.ferritecore.mixin.predicates;

import com.google.common.base.Predicate;
import malte0811.ferritecore.impl.Deduplicator;
import malte0811.ferritecore.util.PredicateHelper;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.multipart.ConditionOr;
import net.minecraft.client.renderer.block.model.multipart.ICondition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConditionOr.class, priority = 2000)
public class OrConditionMixin {
    @Shadow
    @Final
    private Iterable<? extends ICondition> conditions;

    /**
     * @reason Use cached result predicates
     * @author malte0811
     */
    @Overwrite
    public Predicate<IBlockState> getPredicate(BlockStateContainer stateContainer) {
        return Deduplicator.or(PredicateHelper.toCanonicalList(conditions, stateContainer));
    }
}
