package org.embeddedt.vintagefix;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = "vintagefix", name = "VintageFix", version = Tags.VERSION, dependencies = "after:foamfix@[INCOMPATIBLE];" +
    "after:loliasm@[" + VintageFix.REQUIRED_XASM_VERSION + ",);" +
    "after:normalasm@[" + VintageFix.REQUIRED_XASM_VERSION + ",)")
public class VintageFix {

    public static final Logger LOGGER = LogManager.getLogger("VintageFix");
    public static final String REQUIRED_XASM_VERSION = "5.10";

    public static final File MY_DIR = new File(Launch.minecraftHome, "vintagefix");
    public static final File OUT_DIR = new File(MY_DIR, "out");
    public static final File CACHE_DIR = new File(MY_DIR, "transformerCache");

    @Mod.EventHandler
    @SuppressWarnings("unused")
    public void init(FMLConstructionEvent ev) {
        if(FMLCommonHandler.instance().getSide() == Side.CLIENT)
            MinecraftForge.EVENT_BUS.register(new VintageFixClient());
    }
}
