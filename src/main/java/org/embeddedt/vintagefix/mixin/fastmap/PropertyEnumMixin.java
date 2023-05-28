package org.embeddedt.vintagefix.mixin.fastmap;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.properties.PropertyEnum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(PropertyEnum.class)
public class PropertyEnumMixin<T> {
    @Shadow
    @Final
    private ImmutableSet<T> allowedValues;
    @Shadow
    @Final
    private Map<String, T> nameToValue;
    private int vfix_hashCode;

    /**
     * @author embeddedt
     * @reason cache
     */
    @Overwrite
    public int hashCode() {
        if(vfix_hashCode == 0) {
            int i = super.hashCode();
            i = 31 * i + this.allowedValues.hashCode();
            i = 31 * i + this.nameToValue.hashCode();
            vfix_hashCode = i;
        }
        return vfix_hashCode;
    }
}
