package org.embeddedt.vintagefix.mixin.resourcepacks;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.ResourceIndex;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.core.VintageFixCore;
import org.embeddedt.vintagefix.dynamicresources.ICachedResourcePack;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Stream;

@Mixin(DefaultResourcePack.class)
@ClientOnlyMixin
public abstract class MixinDefaultResourcePack implements ICachedResourcePack {
    @Shadow
    @Final
    private ResourceIndex resourceIndex;

    @Shadow
    @Nullable
    protected abstract InputStream getResourceStream(ResourceLocation location);

    private static final ObjectOpenHashSet<String> containedPaths = new ObjectOpenHashSet<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if(containedPaths.size() == 0) {
            Collection<String> paths;
            try {
                paths = ResourcePackHelper.getAllPaths((DefaultResourcePack) (Object) this, p -> true);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            containedPaths.addAll(paths);
            containedPaths.trim();
        }
    }

    @Nullable
    @Override
    public Stream<String> getAllPaths() {
        if(containedPaths.size() == 0)
            return null;
        return containedPaths.stream();
    }

    /**
     * @author embeddedt
     * @reason use cached list of class resources instead of rechecking
     */
    @Overwrite
    public boolean resourceExists(ResourceLocation location)
    {
        String path = "/assets/" + location.getNamespace() + "/" + location.getPath();
        if(containedPaths.contains(path) || this.resourceIndex.isFileExisting(location))
            return true;
        if(VintageFixCore.OPTIFINE)
            return this.getResourceStream(location) != null;
        return false;
    }
}
