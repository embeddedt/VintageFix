package org.embeddedt.ferritecore;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.ferritecore.Tags;

@Mod(modid = "martensitecore", name = "MartensiteCore", version = Tags.VERSION)
public class MartensiteCore {

    public static final Logger LOGGER = LogManager.getLogger("MartensiteCore");

    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void init(FMLConstructionEvent ev) {
        if(FMLCommonHandler.instance().getSide() == Side.CLIENT)
            MinecraftForge.EVENT_BUS.register(new MartensiteCoreClient());
    }
}
