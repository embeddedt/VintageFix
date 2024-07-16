package org.embeddedt.vintagefix.mixin.blockstates;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.properties.PropertyBool;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(value = PropertyBool.class, priority = 800)
public class PropertyBoolMixin {
    @Shadow
    @Final
    @Mutable
    private ImmutableSet<Boolean> allowedValues;

    private static final ImmutableSet<Boolean> ALLOWED_PROP_VALUES = ImmutableSet.of(Boolean.TRUE, Boolean.FALSE);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void staticValues(CallbackInfo ci) {
        this.allowedValues = ALLOWED_PROP_VALUES;
    }

    /**
     * @author embeddedt
     * @reason use static version of collection
     */
    @Overwrite
    public Collection<Boolean> getAllowedValues() {
        return ALLOWED_PROP_VALUES;
    }

    /**
     * @author embeddedt
     * @reason remove allowedValues comparison (Mojank...)
     */
    @Overwrite
    public boolean equals(Object other) {
        if(this == other)
            return true;
        return other instanceof PropertyBool && super.equals(other);
    }

    /**
     * @author embeddedt
     * @reason do not waste time hashing a map that is always the same
     */
    @Overwrite
    public int hashCode() {
        return super.hashCode();
    }
}
