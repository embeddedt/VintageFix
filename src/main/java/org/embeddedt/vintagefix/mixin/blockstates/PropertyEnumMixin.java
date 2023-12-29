package org.embeddedt.vintagefix.mixin.blockstates;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyHelper;
import net.minecraft.util.IStringSerializable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = PropertyEnum.class, priority = 800)
public abstract class PropertyEnumMixin<T extends Enum<T> & IStringSerializable> extends PropertyHelper<T> {
    @Shadow
    @Final
    private ImmutableSet<T> allowedValues;
    @Shadow
    @Final
    private Map<String, T> nameToValue;
    private int vfix_hashCode;

    protected PropertyEnumMixin(String name, Class<T> valueClass) {
        super(name, valueClass);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void computeHash(CallbackInfo ci) {
        int i = super.hashCode();
        i = 31 * i + this.allowedValues.hashCode();
        i = 31 * i + this.nameToValue.hashCode();
        vfix_hashCode = i;
    }

    /**
     * @author asiekierka
     * @reason If the value classes are equal, we can skip checking all the entries in the maps.
     */
    @Redirect(method = "equals", at = @At(value = "INVOKE", target = "Ljava/util/Map;equals(Ljava/lang/Object;)Z", ordinal = 0))
    private boolean areNameMapsEqual(Map map, Object o, Object p_equals_1_) {
        return ((PropertyEnum<?>)p_equals_1_).getValueClass() == this.getValueClass() || map.equals(o);
    }

    /**
     * @author embeddedt
     * @reason cache
     */
    @Overwrite
    public int hashCode() {
        return vfix_hashCode;
    }
}
