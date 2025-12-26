package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WrappingModelRegistry extends RegistrySimple<ModelResourceLocation, IBakedModel> {
    private enum UniverseVisibility {
        /**
         * Mod cannot see any view of the universe of model locations.
         */
        NONE,
        /**
         * Mod can see its own model locations and those of dependencies/dependents.
         */
        SELF_AND_DEPS,
        /**
         * Mod can see every model location.
         */
        EVERYTHING
    }
    private static final Map<String, UniverseVisibility> MOD_VISIBILITY_CONFIGURATION = ImmutableMap.<String, UniverseVisibility>builder()
        .put("opencomputers", UniverseVisibility.SELF_AND_DEPS)
        .put("refinedstorage", UniverseVisibility.SELF_AND_DEPS)
        .put("cabletiers", UniverseVisibility.SELF_AND_DEPS)
        .put("thebetweenlands", UniverseVisibility.NONE)
        .build();
    private static final boolean ENABLE_LOAD_TRACKING = false;

    private final IRegistry<ModelResourceLocation, IBakedModel> delegate;
    private final MutableGraph<String> dependencyGraph;
    private final Set<ModelResourceLocation> requestedModels = new ObjectOpenHashSet<>();

    public WrappingModelRegistry(IRegistry<ModelResourceLocation, IBakedModel> registry) {
        this.delegate = registry;
        this.dependencyGraph = buildDependencyGraph();
    }

    public void resetTracking() {
        this.requestedModels.clear();
    }

    public void reportIssues(ModContainer mc) {
        if (!ENABLE_LOAD_TRACKING || MOD_VISIBILITY_CONFIGURATION.containsKey(mc.getModId())) {
            return;
        }
        if (this.requestedModels.size() >= (3 * ModelLocationInformation.allKnownModelLocations.size()) / 4) {
            VintageFix.LOGGER.fatal("Mod '{}' has requested over 75% of the known models to be loaded, this will be harmful to launch time and potentially indicates a buggy ModelBakeEvent. Please report to VintageFix.", mc.getModId());
        }
    }

    @Nullable
    @Override
    public IBakedModel getObject(ModelResourceLocation name) {
        if (ENABLE_LOAD_TRACKING && name != null) {
            this.requestedModels.add(name);
        }
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

    private MutableGraph<String> buildDependencyGraph() {
        MutableGraph<String> dependencyGraph = GraphBuilder.undirected().build();
        for (ModContainer mc : Loader.instance().getModList()) {
            dependencyGraph.addNode(mc.getModId());
            for (ArtifactVersion dep : mc.getDependencies()) {
                dependencyGraph.addNode(dep.getLabel());
            }
        }
        Set<String> namespacesWithModels = new ObjectOpenHashSet<>();
        for (ModelResourceLocation loc : ModelLocationInformation.allKnownModelLocations) {
            namespacesWithModels.add(loc.getNamespace());
        }
        for(String id : dependencyGraph.nodes()) {
            ModContainer mc = Loader.instance().getIndexedModList().get(id);
            if (mc != null) {
                for(ArtifactVersion version : mc.getDependencies()) {
                    // avoid self-loops
                    if(!Objects.equals(id, version.getLabel()) && !version.getLabel().equals("minecraft") && namespacesWithModels.contains(version.getLabel()))
                        dependencyGraph.putEdge(id, version.getLabel());
                }
            }
        }
        return dependencyGraph;
    }

    private Set<String> computeVisibleModIds(String modId) {
        Set<String> deps;
        try {
            deps = this.dependencyGraph.adjacentNodes(modId);
        } catch (IllegalArgumentException e) {
            deps = ImmutableSet.of();
        }
        if (deps.isEmpty()) {
            // avoid extra work below
            return ImmutableSet.of(modId);
        }
        ObjectOpenHashSet<String> set = new ObjectOpenHashSet<>();
        set.add(modId);
        set.addAll(deps);
        return ImmutableSet.copyOf(set);
    }

    private Set<ModelResourceLocation> getExtraKeysForCaller(ModContainer container) {
        if(container == null) {
            return null;
        }
        String modId = container.getModId();
        UniverseVisibility config = MOD_VISIBILITY_CONFIGURATION.getOrDefault(modId, UniverseVisibility.EVERYTHING);
        if (config == UniverseVisibility.NONE) {
            return null;
        }
        final Set<String> modIdsToInclude = computeVisibleModIds(modId);
        Set<ModelResourceLocation> ourModelLocations;
        if (config == UniverseVisibility.SELF_AND_DEPS) {
            VintageFix.LOGGER.debug("Mod {} is restricted to seeing models from mods: [{}]", modId, String.join(", ", modIdsToInclude));
            ourModelLocations = Sets.filter(ModelLocationInformation.allKnownModelLocations, loc -> modIdsToInclude.contains(loc.getNamespace()));
        } else {
            ourModelLocations = Collections.unmodifiableSet(ModelLocationInformation.allKnownModelLocations);
        }
        return ourModelLocations;
    }

    @Override
    public Iterator<IBakedModel> iterator() {
        return this.delegate.iterator();
    }
}
