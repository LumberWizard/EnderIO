package crazypants.enderio.base.init;

import javax.annotation.Nonnull;

import com.enderio.core.common.vecmath.Vector4f;

import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.Log;
import crazypants.enderio.base.diagnostics.EnderIOCrashCallable;
import crazypants.enderio.base.filter.capability.CapabilityFilterHolder;
import crazypants.enderio.base.integration.top.TOPUtil;
import crazypants.enderio.base.invpanel.capability.CapabilityDatabaseHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class CommonProxy {

  protected long serverTickCount = 0;
  protected long clientTickCount = 0;
  protected final TickTimer tickTimer = new TickTimer();

  public CommonProxy() {
  }

  public World getClientWorld() {
    return null;
  }

  public EntityPlayer getClientPlayer() {
    return null;
  }

  public double getReachDistanceForPlayer(EntityPlayer entityPlayer) {
    return 5;
  }

  public void init(@Nonnull FMLPreInitializationEvent event) {
    TOPUtil.create();
    CapabilityFilterHolder.register();
    CapabilityDatabaseHandler.register();
  }

  public void init(@Nonnull FMLInitializationEvent event) {
    MinecraftForge.EVENT_BUS.register(tickTimer);
    // registerCommands(); // debug command disabled because it is not needed at the moment
  }

  public void init(@Nonnull FMLPostInitializationEvent event) {
  }

  public void stopWithErrorScreen(String... message) {
    EnderIOCrashCallable.registerStopScreenMessage(message);
    for (String string : message) {
      Log.error(string);
    }
    throw new RuntimeException("Ender IO cannot continue, see error messages above");
  }

  protected void registerCommands() {
  }

  public long getTickCount() {
    return serverTickCount;
  }

  public long getServerTickCount() {
    return serverTickCount;
  }

  public boolean isAnEiInstalled() {
    return false;
  }

  public void setInstantConfusionOnPlayer(@Nonnull EntityPlayer ent, int duration) {
    ent.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, duration, 1, true, true));
  }

  protected void onServerTick() {
    ++serverTickCount;
  }

  protected void onClientTick() {
  }

  public final class TickTimer {

    @SubscribeEvent
    public void onTick(@Nonnull ServerTickEvent evt) {
      if (evt.phase == Phase.END) {
        onServerTick();
      }
    }

    @SubscribeEvent
    public void onTick(@Nonnull ClientTickEvent evt) {
      if (evt.phase == Phase.END) {
        onClientTick();
      }
    }
  }

  private static final String TEXTURE_PATH = ":textures/gui/40/";
  private static final String TEXTURE_EXT = ".png";

  public @Nonnull ResourceLocation getGuiTexture(@Nonnull String name) {
    return new ResourceLocation(EnderIO.DOMAIN + TEXTURE_PATH + name + TEXTURE_EXT);
  }

  public void markBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Vector4f color) {
  }

  public boolean isDedicatedServer() {
    return true;
  }

  public CreativeTabs getCreativeTab(@Nonnull ItemStack stack) {
    return null;
  }

}
