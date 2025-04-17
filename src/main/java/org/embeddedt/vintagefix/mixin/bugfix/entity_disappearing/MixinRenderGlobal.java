package org.embeddedt.vintagefix.mixin.bugfix.entity_disappearing;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

@Mixin(RenderGlobal.class)
@ClientOnlyMixin
public abstract class MixinRenderGlobal {
    @Shadow @Final
    private RenderManager renderManager;

    @Shadow @Final private Minecraft mc;

    @Shadow private int countEntitiesRendered;

    @Shadow private WorldClient world;

    @Shadow protected abstract boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera);

    @Shadow
    private ViewFrustum viewFrustum;

    /**
     * @author embeddedt
     * @reason disable vanilla rendering
     */
    @Redirect(method = "renderEntities", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;")), at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0))
    private Iterator<RenderGlobal.ContainerLocalRenderInformation> skipVanillaIterator(List<RenderGlobal.ContainerLocalRenderInformation> list) {
        return this.viewFrustum.renderChunks.length > 0 ? Collections.emptyIterator() : list.iterator();
    }

    private Iterator<Entity> getLoadedEntityIterator() {
        // Iterate directly over chunk entity lists where possible - mods may create multipart entities that are not
        // added to the main loadedEntityList.
        if (this.world.getChunkProvider() instanceof AccessorChunkProviderClient) {
            Long2ObjectMap<Chunk> loadedChunks = ((AccessorChunkProviderClient)this.world.getChunkProvider()).vfix$getLoadedChunks();
            List<Entity> allEntities = new ArrayList<>(this.world.loadedEntityList.size());
            Consumer<Entity> addEntity = allEntities::add;
            for (Chunk chunk : loadedChunks.values()) {
                ClassInheritanceMultiMap<Entity>[] entityMaps = chunk.getEntityLists();
                for (ClassInheritanceMultiMap<Entity> map : entityMaps) {
                    if (map.isEmpty()) {
                        continue;
                    }
                    map.forEach(addEntity);
                }
            }
            return allEntities.iterator();
        } else {
            // Best we can do is the loaded entity list - this will miss some multipart entities
            return this.world.loadedEntityList.iterator();
        }
    }

    /**
     * @author embeddedt
     * @reason reimplement entity render loop because vanilla's relies on the renderInfos list
     */
    @Inject(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", ordinal = 0))
    private void renderEntities(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci,
                                @Local(ordinal = 1) List<Entity> outlineEntityList,
                                @Local(ordinal = 2) List<Entity> multipassEntityList,
                                @Local(ordinal = 0) double renderViewX,
                                @Local(ordinal = 1) double renderViewY,
                                @Local(ordinal = 2) double renderViewZ) {
        if (this.viewFrustum.renderChunks.length == 0) {
            // The vanilla chunk renderer is not in use, do not run our logic
            return;
        }
        int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
        EntityPlayerSP player = this.mc.player;
        BlockPos.MutableBlockPos entityBlockPos = new BlockPos.MutableBlockPos();
        Iterator<Entity> loadedEntityIterator = getLoadedEntityIterator();
        while (loadedEntityIterator.hasNext()) {
            Entity entity = loadedEntityIterator.next();
            // Skip entities that shouldn't render in this pass
            if(!entity.shouldRenderInPass(pass)) {
                continue;
            }

            // Do regular vanilla checks for visibility
            if(!this.renderManager.shouldRender(entity, camera, renderViewX, renderViewY, renderViewZ) && !entity.isRidingOrBeingRiddenBy(player)) {
                continue;
            }

            boolean isSleeping = renderViewEntity instanceof EntityLivingBase && ((EntityLivingBase) renderViewEntity).isPlayerSleeping();

            if ((entity != renderViewEntity || this.mc.gameSettings.thirdPersonView != 0 || isSleeping)
                && (entity.posY < 0.0D || entity.posY >= 256.0D || this.world.isBlockLoaded(entityBlockPos.setPos(entity))))
            {
                ++this.countEntitiesRendered;
                this.renderManager.renderEntityStatic(entity, partialTicks, false);

                if (this.isOutlineActive(entity, renderViewEntity, camera))
                {
                    outlineEntityList.add(entity);
                }

                if (this.renderManager.isRenderMultipass(entity)) {
                    multipassEntityList.add(entity);
                }
            }
        }
    }
}
