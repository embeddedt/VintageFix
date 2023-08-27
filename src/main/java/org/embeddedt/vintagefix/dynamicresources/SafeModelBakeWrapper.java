package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicBakedModelProvider;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class SafeModelBakeWrapper {
    public static ModelManager theManager;
    private static final ImmutableList<Pair<String, String>> MOD_BAKE_CLASSES = ImmutableList.<Pair<String, String>>builder()
        .add(Pair.of("tschipp.hardcoreitemstages.ItemStageEventHandler", "onModelBakeEvent"))
        .build();

    public static void setup() {
        for(Pair<String, String> pair : MOD_BAKE_CLASSES) {
            try {
                Class<?> clz;
                try {
                    clz = Class.forName(pair.getLeft());
                } catch(ClassNotFoundException | LinkageError ignored) {
                    continue;
                }
                Method theMethod = clz.getDeclaredMethod(pair.getRight(), ModelBakeEvent.class);
                if(!Modifier.isStatic(theMethod.getModifiers())) {
                    VintageFix.LOGGER.warn("Non-static methods currently not supported (on {})", pair);
                    continue;
                }
                theMethod.setAccessible(true);
                MethodHandle eventHook = MethodHandles.lookup().unreflect(theMethod);
                MinecraftForge.EVENT_BUS.register(new WrappingHandler(eventHook));
                VintageFix.LOGGER.info("Registered model bake compat for {}", pair.getLeft());
            } catch(ReflectiveOperationException | RuntimeException e) {
                VintageFix.LOGGER.error("Failed to setup model bake wrapper for {}", pair);
            }
        }
    }

    public static class WrappingHandler {
        private final MethodHandle modEventHandler;

        WrappingHandler(MethodHandle modEventHandler) {
            this.modEventHandler = modEventHandler;
        }

        private int recursion = 0;

        @SubscribeEvent
        public void onDynBake(DynamicModelBakeEvent e) {
            if(!(e.location instanceof ModelResourceLocation) || recursion > 0 || SafeModelBakeWrapper.theManager == null)
                return;
            recursion++;
            try {
                RegistrySimple<ModelResourceLocation, IBakedModel> reg = new RegistrySimple<>();
                reg.putObject((ModelResourceLocation)e.location, e.bakedModel);
                ModelBakeEvent event = new ModelBakeEvent(
                    SafeModelBakeWrapper.theManager,
                    reg,
                    null
                );
                try {
                    this.modEventHandler.invokeExact(event);
                } catch(Throwable ex) {
                    VintageFix.LOGGER.error("Exception running mod event handler", ex);
                }
                if(reg.getKeys().size() == 1) {
                    // fast path
                    IBakedModel m = reg.getObject((ModelResourceLocation)e.location);
                    if(m != null)
                        e.bakedModel = m;
                } else {
                    // slow path
                    for(ModelResourceLocation key : reg.getKeys()) {
                        IBakedModel m = reg.getObject(key);
                        if(m == null)
                            continue;
                        if(Objects.equals(key, e.location)) {
                            e.bakedModel = m;
                        } else {
                            DynamicBakedModelProvider.instance.putObject(key, m);
                        }
                    }
                }
            } finally {
                recursion--;
            }
        }
    }
}
