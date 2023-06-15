package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.dynamicresources.helpers.DefaultPackAdapter;
import org.embeddedt.vintagefix.dynamicresources.helpers.FilePackAdapter;
import org.embeddedt.vintagefix.dynamicresources.helpers.FolderPackAdapter;
import org.embeddedt.vintagefix.dynamicresources.helpers.RemappingAdapter;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourcePackHelper {
    private static final Map<Class<? extends IResourcePack>, Adapter<? extends IResourcePack>> ADAPTERS = new Object2ObjectArrayMap<>();

    public static <T extends IResourcePack> void registerAdapter(Class<T> clz, Adapter<T> adapter) {
        ADAPTERS.put(clz, adapter);
    }

    static {
        registerAdapter(LegacyV2Adapter.class, new RemappingAdapter<>(pack -> {
            return ObfuscationReflectionHelper.getPrivateValue(LegacyV2Adapter.class, pack, "field_191383_a");
        }));
        registerAdapter(DefaultResourcePack.class, new DefaultPackAdapter());
        registerAdapter(FileResourcePack.class, new FilePackAdapter());
        registerAdapter(FolderResourcePack.class, new FolderPackAdapter());
    }

    public static Collection<IResourcePack> getAllPacks(SimpleReloadableResourceManager manager) {
        Set<IResourcePack> resourcePacks = new LinkedHashSet<>();
        Map<String, FallbackResourceManager> domainManagers = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, manager, "field_110548_a");
        for (FallbackResourceManager fallback : domainManagers.values()) {
            List<IResourcePack> fallbackPacks = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, fallback, "field_110540_a");
            resourcePacks.addAll(fallbackPacks);
        }
        return resourcePacks;
    }

    public static Collection<String> getAllPaths(SimpleReloadableResourceManager manager, Predicate<String> filter) {
        Collection<IResourcePack> resourcePacks = getAllPacks(manager);
        Set<String> paths = new ObjectOpenHashSet<>();
        for (IResourcePack pack : resourcePacks) {
            try {
                paths.addAll(getAllPaths(pack, filter));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }

    @SuppressWarnings("unchecked")
    private static <T extends IResourcePack> Collection<String> applyAdapter(IResourcePack pack, Predicate<String> filter, Adapter<T> adapter) throws IOException {
        List<String> paths = new ArrayList<>();
        Iterator<String> incomingPaths = adapter.getAllPaths((T)pack, filter);
        while(incomingPaths.hasNext())
            paths.add(incomingPaths.next());
        return paths;
    }

    public static Collection<String> getAllPaths(IResourcePack pack, Predicate<String> filter) throws IOException {
        if(pack instanceof ICachedResourcePack) {
            ICachedResourcePack cachePack = (ICachedResourcePack)pack;
            Stream<String> paths = cachePack.getAllPaths();
            if(paths != null)
                return paths.filter(filter).collect(Collectors.toList());
        }
        for(Map.Entry<Class<? extends IResourcePack>, Adapter<? extends IResourcePack>> adapterEntry : ADAPTERS.entrySet()) {
            if(adapterEntry.getKey().isAssignableFrom(pack.getClass())) {
                return applyAdapter(pack, filter, adapterEntry.getValue());
            }
        }
        // no adapters found, give up
        VintageFix.LOGGER.warn("Cannot list resources from pack {} ({})", pack.getPackName(), pack.getClass().getName());
        return ImmutableList.of();
    }

    public static final Pattern PATH_TO_RESLOC_REGEX = Pattern.compile("^/?assets/(.+?(?=/))/(.*)$");
    public static final Pattern PATH_TO_SHORT_RESLOC_REGEX = Pattern.compile("^^/?assets/(.+?(?=/))/(?:.+?(?=/))/(.*)\\.(?:[A-Za-z]*)$");

    public enum ResourceLocationMatchType {
        SHORT,
        FULL
    }

    public static ResourceLocation pathToResourceLocation(String path, ResourceLocationMatchType type) {
        Pattern pattern;
        switch(type) {
            default:
            case SHORT:
                pattern = PATH_TO_SHORT_RESLOC_REGEX;
                break;
            case FULL:
                pattern = PATH_TO_RESLOC_REGEX;
                break;
        }
        Matcher matcher = pattern.matcher(path);
        if (matcher.matches())
            return new ResourceLocation(matcher.group(1), matcher.group(2));
        else
            return null;
    }

    @FunctionalInterface
    public interface Adapter<T extends IResourcePack> {
        /**
         * Get all paths matching the filter in the given resource pack.
         * <br>
         * Implementations need to be careful to return an Iterator that will still work after return
         * (hence why the current ones collect to a list first).
         *
         * @param pack resource pack to get paths from
         * @param filter a filter to apply to each path
         * @return an iterator that provides a list of all the paths. May be lazily populated
         * @throws IOException if any error occurs during this process
         */
        Iterator<String> getAllPaths(T pack, Predicate<String> filter) throws IOException;
    }
}
