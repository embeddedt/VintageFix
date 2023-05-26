package org.embeddedt.vintagefix.core;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.embeddedt.vintagefix.transformercache.TransformerCache;
import org.embeddedt.vintagefix.util.DummyList;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("VintageFix")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class VintageFixCore implements IFMLLoadingPlugin, IEarlyMixinLoader {
    public static boolean OPTIFINE;

    public VintageFixCore() {
        try {
            Class<?> transformerClass = Class.forName("zone.rong.loliasm.core.LoliTransformer");
            Field field = transformerClass.getDeclaredField("squashBakedQuads");
            field.setAccessible(true);
            field.setBoolean(null, false);
            System.out.println("Disabled squashBakedQuads due to compatibility issues, please ask Rongmario to fix this someday");
        } catch(ReflectiveOperationException ignored) {}
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch(ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    private static boolean mixinFixApplied = false;

    private static void applyMixinFix() {
        OPTIFINE = classExists("ofdev.launchwrapper.OptifineDevTweakerWrapper") || classExists("optifine.OptiFineForgeTweaker");
        /* https://github.com/FabricMC/Mixin/pull/99 */
        try {
            Field groupMembersField = InjectorGroupInfo.class.getDeclaredField("members");
            groupMembersField.setAccessible(true);
            Field noGroupField = InjectorGroupInfo.Map.class.getDeclaredField("NO_GROUP");
            noGroupField.setAccessible(true);
            InjectorGroupInfo noGroup = (InjectorGroupInfo)noGroupField.get(null);
            groupMembersField.set(noGroup, new DummyList<>());
        } catch(RuntimeException | ReflectiveOperationException ignored) {
        }
    }

    @Override
    public List<String> getMixinConfigs() {
        if(!mixinFixApplied) {
            applyMixinFix();
            mixinFixApplied = true;
        }
        return ImmutableList.of("mixins.vintagefix.json");
    }
}
