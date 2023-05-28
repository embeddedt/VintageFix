package org.embeddedt.vintagefix.mixin.resourcepacks;

import net.minecraft.client.resources.ResourceIndex;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.Map;

@Mixin(ResourceIndex.class)
@ClientOnlyMixin
public class MixinResourceIndex {
    @Redirect(method = "<init>(Ljava/io/File;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object putToMap(Map map, Object k, Object v) {
        if(((File)v).isFile())
            map.put(k, v);
        return null;
    }

    @Redirect(method = "isFileExisting", at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z"))
    private boolean skipExistsCheck(File file) {
        return true;
    }
}
