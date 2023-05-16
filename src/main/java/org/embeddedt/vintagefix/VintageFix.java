package org.embeddedt.vintagefix;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "vintagefix", name = "VintageFix", version = Tags.VERSION)
public class VintageFix {

    public static final Logger LOGGER = LogManager.getLogger("VintageFix");

    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void init(FMLConstructionEvent ev) {
        if(FMLCommonHandler.instance().getSide() == Side.CLIENT)
            MinecraftForge.EVENT_BUS.register(new VintageFixClient());
    }
}
