package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.Item;
import net.minecraftforge.client.ItemModelMesherForge;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.core.MixinConfigPlugin;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicBakedModelProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ItemBakeThread extends Thread {
    private static volatile ItemBakeThread bakeThread = null;
    private final List<ModelResourceLocation> locations;
    private final DynamicBakedModelProvider myProvider;
    volatile boolean stop = false;

    public static void restartBake() {
        stopAndJoin();
        if(!MixinConfigPlugin.isMixinClassApplied("mixin.dynamic_resources.background_item_bake.BakeMixin"))
            return;
        ItemModelMesherForge immf = (ItemModelMesherForge)net.minecraft.client.Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
        Map<IRegistryDelegate<Item>, Int2ObjectMap<ModelResourceLocation>> theMap = ObfuscationReflectionHelper.getPrivateValue(ItemModelMesherForge.class, immf, "locations");
        List<ModelResourceLocation> theList = new ArrayList<>();
        for(Int2ObjectMap<ModelResourceLocation> innerMap : theMap.values()) {
            theList.addAll(innerMap.values());
        }
        bakeThread = new ItemBakeThread(theList);
        bakeThread.start();
    }

    ItemBakeThread(List<ModelResourceLocation> locations) {
        this.locations = locations;
        this.myProvider = DynamicBakedModelProvider.instance;
        this.setName("VintageFix item baking thread");
        this.setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Iterator<ModelResourceLocation> iterator = locations.iterator();
        int numBaked = 0;
        int total = locations.size();
        long lastPrint = System.nanoTime();
        long printDelta = TimeUnit.MILLISECONDS.toNanos(5000);
        VintageFix.LOGGER.info("Baking {} models in background", total);
        while(!stop && iterator.hasNext()) {
            ModelResourceLocation location = iterator.next();
            try {
                IBakedModel theModel = myProvider.getObject(location);
                if(theModel != null)
                    myProvider.putObject(location, theModel);
            } catch(Throwable e) {
                VintageFix.LOGGER.error("Error baking {}: {}", location, e);
            }
            numBaked++;
            if((numBaked % 10) == 0 && (System.nanoTime() - lastPrint) >= printDelta) {
                lastPrint = System.nanoTime();
                VintageFix.LOGGER.info(String.format("Item baking at %.02f%%",  ((float)numBaked)/total * 100f));
            }
        }
        stopwatch.stop();
        if(!stop)
            VintageFix.LOGGER.info("Item baking finished in {}", stopwatch);
    }

    public static void stopAndJoin() {
        if(bakeThread != null) {
            bakeThread.stop = true;
            while(bakeThread.isAlive()) {
                try {
                    bakeThread.join();
                } catch(InterruptedException ignored) {
                }
            }
            bakeThread = null;
        }
    }

    public static class ReloadListener implements ISelectiveResourceReloadListener {
        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
            if(resourcePredicate.test(VanillaResourceType.MODELS)) {
                ItemBakeThread.restartBake();
            }
        }
    }
}
