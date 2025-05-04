package org.embeddedt.vintagefix.mixin.textures;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureUtil.class)
@ClientOnlyMixin
public class MixinTextureUtil {
    @Redirect(method = "blendColors", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lnet/minecraft/client/renderer/texture/TextureUtil;MIPMAP_BUFFER:[I"), require = 0)
    private static int[] useLocalMipmapBuffer(@Share("mipmapBuffer") LocalRef<int[]> buffer) {
        int[] buf = buffer.get();
        if (buf == null) {
            buf = new int[4];
            buffer.set(buf);
        }
        return buf;
    }
}
