package org.embeddedt.vintagefix.mixin.bugfix.slow_tps_catchup;

import net.minecraft.crash.CrashReport;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ReportedException;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.Logger;
import org.embeddedt.vintagefix.VintageFix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Use a low priority mixin so our overwrite can be modified.
 */
@Mixin(value = MinecraftServer.class, priority = 200)
public abstract class MinecraftServerMixin
{
    @Shadow
    public abstract boolean init() throws IOException;

    @Shadow
    public static long getCurrentTimeMillis()
    {
        return 0;
    }

    @Shadow
    protected long currentTime;

    @Shadow
    @Final
    private ServerStatusResponse statusResponse;

    @Shadow
    private String motd;

    @Shadow
    public abstract void applyServerIconToResponse(ServerStatusResponse response);

    @Shadow
    private boolean serverRunning;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract void tick();

    @Shadow
    private long timeOfLastWarning;

    @Shadow
    private boolean serverIsRunning;

    @Shadow
    public abstract void finalTick(CrashReport report);

    @Shadow
    public abstract CrashReport addServerInfoToCrashReport(CrashReport report);

    @Shadow
    public abstract File getDataDirectory();

    @Shadow
    public abstract void stopServer();

    @Shadow
    private boolean serverStopped;

    @Shadow
    public abstract void systemExitNow();

    /**
     * @author Michael Kreitzer
     * @reason I would have liked to only replace what was inside the "try" block, but there was no reasonable way
     * to do that. This is a complete rewrite of this function outside of error handling.
     */
    @Overwrite
    public void run()
    {
        try
        {
            if (this.init())
            {
                VintageFix.LOGGER.info("Using alternate server main loop.");

                net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStarted();

                long referenceTime = getCurrentTimeMillis();
                this.currentTime = referenceTime;
                this.statusResponse.setServerDescription(new TextComponentString(this.motd));
                this.statusResponse.setVersion(new ServerStatusResponse.Version("1.12.2", 340));
                this.applyServerIconToResponse(this.statusResponse);

                while (this.serverRunning) {
                    long before = getCurrentTimeMillis();
                    this.currentTime = before;
                    this.tick();
                    long after = getCurrentTimeMillis();
                    long tickLength = after - before;
                    long runningBehind = before - referenceTime;

                    if (runningBehind > 2000L && after - this.timeOfLastWarning >= 15000L) {
                        LOGGER.warn("Can\'t keep up! Did the system time change, or is the server overloaded? Running {}ms behind, skipping {} tick(s)", runningBehind, (runningBehind - 2000L - 1L) / 50L + 1L);
                        referenceTime = before - 2000L;
                        runningBehind = 2000L;
                        this.timeOfLastWarning = after;
                    }

                    if (tickLength < 0L || runningBehind < -50L) {
                        LOGGER.warn("Time ran backwards! Did the system time change?");
                        tickLength = 0L;
                        runningBehind = 0L;
                        referenceTime = before;
                    }

                    long sleepTime = 50L - tickLength - runningBehind;
                    if (sleepTime > 0L) {
                        Thread.sleep(sleepTime);
                    }
                    referenceTime += 50L; // Keep track of what time it should be with the current number of executed ticks

                    this.serverIsRunning = true;
                }

                net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopping();
                net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
            }
            else
            {
                net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
                this.finalTick(null);
            }
        }
        catch (net.minecraftforge.fml.common.StartupQuery.AbortedException e)
        {
            // ignore silently
            net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
        }
        catch (Throwable throwable1)
        {
            LOGGER.error("Encountered an unexpected exception", throwable1);
            CrashReport crashreport;

            if (throwable1 instanceof ReportedException)
            {
                crashreport = this.addServerInfoToCrashReport(((ReportedException)throwable1).getCrashReport());
            }
            else
            {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
            }

            File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.saveToFile(file1))
            {
                LOGGER.error("This crash report has been saved to: {}", file1.getAbsolutePath());
            }
            else
            {
                LOGGER.error("We were unable to save this crash report to disk.");
            }

            net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
            this.finalTick(crashreport);
        }
        finally
        {
            try
            {
                this.stopServer();
            }
            catch (Throwable throwable)
            {
                LOGGER.error("Exception stopping the server", throwable);
            }
            finally
            {
                net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopped();
                this.serverStopped = true;
                this.systemExitNow();
            }
        }
    }
}
