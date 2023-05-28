package org.embeddedt.vintagefix.mixin.resourcepacks;

import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Mixin(FallbackResourceManager.class)
@ClientOnlyMixin
public abstract class MixinFallbackResourceManager {
    @Shadow
    static ResourceLocation getLocationMcmeta(ResourceLocation location) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void checkResourcePath(ResourceLocation p_188552_1_) throws IOException;

    @Shadow
    @Final
    protected List<IResourcePack> resourcePacks;

    @Shadow
    protected abstract InputStream getInputStream(ResourceLocation location, IResourcePack resourcePack) throws IOException;

    @Shadow
    @Final
    private MetadataSerializer frmMetadataSerializer;

    /**
     * @author embeddedt
     * @reason avoid resourceExists (disabled for now until I know whether it helps performance)
     */
    /*
    @Overwrite
    public IResource getResource(ResourceLocation location) throws IOException {
        this.checkResourcePath(location);
        InputStream metaStream = null;
        ResourceLocation metaLocation = getLocationMcmeta(location);

        for (int i = this.resourcePacks.size() - 1; i >= 0; --i) {
            IResourcePack candidatePack = this.resourcePacks.get(i);
            try {
                InputStream stream = this.getInputStream(metaLocation, candidatePack);
                if(metaStream != null)
                    IOUtils.closeQuietly(metaStream);
                metaStream = stream;
            } catch(IOException ignored) {
                // failed to open it, no problem
            }
            try {
                InputStream mainStream = this.getInputStream(location, candidatePack);
                return new SimpleResource(candidatePack.getPackName(), location, mainStream, metaStream, this.frmMetadataSerializer);
            } catch(IOException ignored) {
                // failed to open it, no problem
            }
        }
        throw new FileNotFoundException(location.toString());
    }
     */
}
