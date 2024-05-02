package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;

public class WrappingModelRegistry extends RegistrySimple<ModelResourceLocation, IBakedModel> {
    private final IRegistry<ModelResourceLocation, IBakedModel> delegate;

    public WrappingModelRegistry(IRegistry<ModelResourceLocation, IBakedModel> registry) {
        this.delegate = registry;
    }

    @Nullable
    @Override
    public IBakedModel getObject(ModelResourceLocation name) {
        return this.delegate.getObject(name);
    }

    @Override
    public void putObject(ModelResourceLocation key, IBakedModel value) {
        this.delegate.putObject(key, value);
    }

    @Override
    public Set<ModelResourceLocation> getKeys() {
        ModContainer container = Loader.instance().activeModContainer();
        Set<ModelResourceLocation> extraSet = getExtraKeysForCaller(container);
        Set<ModelResourceLocation> currentKeys = this.delegate.getKeys();
        if(extraSet == null)
            return currentKeys;
        return Sets.union(extraSet, currentKeys);
    }

    @Override
    public boolean containsKey(ModelResourceLocation key) {
        return ModelLocationInformation.allKnownModelLocations.contains(key);
    }

    private static final ImmutableSet<String> MODS_WITH_ITERATING_BAKE_EVENT = ImmutableSet.of("rebornmod", "secretroomsmod", "extratrees");

    private Set<ModelResourceLocation> getExtraKeysForCaller(ModContainer container) {
        if(container == null) {
            return null;
        }
        if(!MODS_WITH_ITERATING_BAKE_EVENT.contains(container.getModId())) {
            VintageFix.LOGGER.warn("Mod '{}' is attempting to iterate the model registry, but is not whitelisted in VintageFix. This may cause glitches if it wraps its own models.", container.getModId());
            return null;
        }
        // add keys from this mod
        return Sets.filter(ModelLocationInformation.allKnownModelLocations, loc -> container.getModId().equals(loc.getNamespace()));
    }

    @Override
    public Iterator<IBakedModel> iterator() {
        return this.delegate.iterator();
    }
}
