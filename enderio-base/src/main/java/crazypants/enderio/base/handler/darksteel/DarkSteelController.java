package crazypants.enderio.base.handler.darksteel;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.ItemUtil;
import com.enderio.core.common.util.NNList;
import com.enderio.core.common.util.NNList.Callback;
import com.enderio.core.common.util.NullHelper;
import com.mojang.authlib.GameProfile;

import crazypants.enderio.api.upgrades.IDarkSteelItem;
import crazypants.enderio.api.upgrades.IDarkSteelUpgrade;
import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.config.Config;
import crazypants.enderio.base.handler.darksteel.PacketUpgradeState.Type;
import crazypants.enderio.base.integration.top.TheOneProbeUpgrade;
import crazypants.enderio.base.item.darksteel.upgrade.elytra.ElytraUpgrade;
import crazypants.enderio.base.item.darksteel.upgrade.energy.EnergyUpgradeManager;
import crazypants.enderio.base.item.darksteel.upgrade.glider.GliderUpgrade;
import crazypants.enderio.base.item.darksteel.upgrade.jump.JumpUpgrade;
import crazypants.enderio.base.item.darksteel.upgrade.nightvision.NightVisionUpgrade;
import crazypants.enderio.base.item.darksteel.upgrade.speed.SpeedController;
import crazypants.enderio.base.network.PacketHandler;
import crazypants.enderio.base.power.PowerHandlerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovementInput;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@EventBusSubscriber(modid = EnderIO.MODID)
public class DarkSteelController {

  private static final EnumSet<Type> DEFAULT_ACTIVE = EnumSet.of(Type.SPEED, Type.STEP_ASSIST, Type.JUMP);

  private static class Data {
    private boolean jumpPre;
    private boolean wasJumping;
    private int jumpCount;
    private int ticksSinceLastJump;

    private final @Nonnull Map<UUID, EnumSet<Type>> allActive = new HashMap<UUID, EnumSet<Type>>();

    private boolean nightVisionActive = false;
    private boolean removeNightvision = false;

  }

  private static ThreadLocal<Data> DATA = new ThreadLocal<Data>() {
    @Override
    protected Data initialValue() {
      return new Data();
    }
  };

  private static EnumSet<Type> getActiveSet(EntityPlayer player) {
    EnumSet<Type> active;
    GameProfile gameProfile = player.getGameProfile();
    UUID id = gameProfile.getId();
    active = id == null ? null : DATA.get().allActive.get(id);
    if (active == null) {
      active = DEFAULT_ACTIVE.clone();
      if (id != null) {
        DATA.get().allActive.put(id, active);
      }
    }
    return active;
  }

  public static boolean isActive(EntityPlayer player, Type type) {
    return getActiveSet(player).contains(type);
  }

  public static void setActive(EntityPlayer player, Type type, boolean isActive) {
    EnumSet<Type> set = getActiveSet(player);
    if (isActive) {
      set.add(type);
    } else {
      set.remove(type);
    }
  }

  public static boolean isGlideActive(EntityPlayer player) {
    return isActive(player, Type.GLIDE);
  }

  public static boolean isSpeedActive(EntityPlayer player) {
    return isActive(player, Type.SPEED);
  }

  public static boolean isStepAssistActive(EntityPlayer player) {
    return isActive(player, Type.STEP_ASSIST);
  }

  public static boolean isJumpActive(EntityPlayer player) {
    return isActive(player, Type.JUMP);
  }

  public static boolean isElytraActive(EntityPlayer player) {
    return isActive(player, Type.ELYTRA);
  }

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    EntityPlayer player = event.player;

    if (event.phase == Phase.START && !player.isSpectator()) {
      // boots
      updateStepHeightAndFallDistance(player);

      // leggings
      SpeedController.updateSpeed(player);

      NNList.of(EntityEquipmentSlot.class).apply(new Callback<EntityEquipmentSlot>() {
        @Override
        public void apply(@Nonnull EntityEquipmentSlot slot) {
          ItemStack item = player.getItemStackFromSlot(slot);
          if (item.getItem() instanceof IDarkSteelItem) {
            for (IDarkSteelUpgrade upgrade : UpgradeRegistry.getUpgrades()) {
              if (upgrade.hasUpgrade(item)) {
                upgrade.onPlayerTick(item, (IDarkSteelItem) item.getItem(), player);
              }
            }
          }
        }
      });

    }
  }

  public static boolean isGliderUpgradeEquipped(EntityPlayer player) {
    ItemStack chestPlate = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
    return GliderUpgrade.INSTANCE.hasUpgrade(chestPlate);
  }

  public static boolean isElytraUpgradeEquipped(EntityPlayer player) {
    ItemStack chestPlate = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
    return isElytraUpgradeEquipped(chestPlate);
  }

  public static boolean isElytraUpgradeEquipped(@Nonnull ItemStack chestPlate) {
    return ElytraUpgrade.INSTANCE.hasUpgrade(chestPlate);
  }

  @SubscribeEvent
  public static void onFall(LivingFallEvent event) {
    float distance = event.getDistance();
    if (distance > 3) {
      ItemStack boots = event.getEntityLiving().getItemStackFromSlot(EntityEquipmentSlot.FEET);
      if (boots.getItem() instanceof IDarkSteelItem) {
        int energyStored = EnergyUpgradeManager.getEnergyStored(boots);
        if (energyStored > 0) {
          float toMitigate = distance - 3;
          int energyCost = (int) Math.min(energyStored, Math.ceil(toMitigate * Config.darkSteelFallDistanceCost));
          float mitigated = energyCost / (float) Config.darkSteelFallDistanceCost;
          if (!event.getEntity().world.isRemote) {
            EnergyUpgradeManager.extractEnergy(boots, (IDarkSteelItem) boots.getItem(), energyCost, false);
          }
          if (mitigated < toMitigate) {
            // Log.debug("Mitigating fall damage partially: original=", distance, " mitigated=", mitigated, " remaining=", distance - mitigated, " power used=",
            // energyCost);
            event.setDistance(distance - mitigated);
          } else {
            // Log.debug("Canceling fall damage: original=", distance, " power used=", energyCost);
            event.setCanceled(true);
          }
        }
      }
    }
  }

  private static final float MAGIC_STEP_HEIGHT = 1.0023f;

  private static void updateStepHeightAndFallDistance(EntityPlayer player) {
    if (player.stepHeight < MAGIC_STEP_HEIGHT && !player.isSneaking() && JumpUpgrade.isEquipped(player) && isStepAssistActive(player)) {
      player.stepHeight = MAGIC_STEP_HEIGHT;
    } else if (player.stepHeight == MAGIC_STEP_HEIGHT) {
      player.stepHeight = 0.6F;
    }
  }

  public static void usePlayerEnergy(EntityPlayer player, EntityEquipmentSlot armorSlot, int cost) {
    if (cost == 0) {
      return;
    }
    int remaining = cost;
    if (Config.darkSteelDrainPowerFromInventory) {
      for (ItemStack stack : player.inventory.mainInventory) {
        IEnergyStorage cap = PowerHandlerUtil.getCapability(NullHelper.notnullM(stack, "null stack in main player inventory"));
        if (cap != null && cap.canExtract()) {
          int used = cap.extractEnergy(remaining, false);
          remaining -= used;
          if (remaining <= 0) {
            return;
          }
        }
      }
    }
    if (armorSlot != null && remaining > 0) {
      ItemStack stack = player.getItemStackFromSlot(armorSlot);
      EnergyUpgradeManager.extractEnergy(stack, remaining, false);
    }
  }

  public static int getPlayerEnergy(EntityPlayer player, EntityEquipmentSlot slot) {
    int res = 0;

    if (Config.darkSteelDrainPowerFromInventory) {
      for (ItemStack stack : player.inventory.mainInventory) {
        IEnergyStorage cap = PowerHandlerUtil.getCapability(NullHelper.notnullM(stack, "null stack in main player inventory"));
        if (cap != null && cap.canExtract()) {
          res += cap.extractEnergy(Integer.MAX_VALUE, true);
        }
      }
    }
    if (slot != null) {
      ItemStack stack = player.getItemStackFromSlot(slot);
      res = EnergyUpgradeManager.getEnergyStored(stack);
    }
    return res;
  }

  @SubscribeEvent
  public static void onStartTracking(PlayerEvent.StartTracking event) {
    if (event.getTarget() instanceof EntityPlayerMP) {
      for (PacketUpgradeState.Type type : PacketUpgradeState.Type.values()) {
        PacketHandler.sendTo(new PacketUpgradeState(type, isActive((EntityPlayer) event.getTarget(), type), event.getTarget().getEntityId()),
            (EntityPlayerMP) event.getEntityPlayer());
      }
    }
  }

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  public static void onClientTick(TickEvent.ClientTickEvent event) {
    EntityPlayerSP player = Minecraft.getMinecraft().player;

    if (NullHelper.untrust(player) == null) {
      // Log.warn("(in TickEvent.ClientTickEvent) net.minecraft.client.Minecraft.player is marked @Nonnull but it is null.");
      return;
    }
    if (NullHelper.untrust(player.movementInput) == null) {
      // Log.warn("(in TickEvent.ClientTickEvent) net.minecraft.client.entity.EntityPlayerSP.movementInput is marked @Nonnull but it is null.");
      return;
    }

    if (event.phase != TickEvent.Phase.END) {
      DATA.get().jumpPre = player.movementInput.jump;
      return;
    }

    updateNightvision(player);
    if (player.capabilities.isFlying) {
      return;
    }

    MovementInput input = player.movementInput;
    boolean jumpHandled = false;
    if (input.jump && (!DATA.get().wasJumping || DATA.get().ticksSinceLastJump > 5)) {
      jumpHandled = doJump(player);
    }

    if (!jumpHandled && input.jump && !DATA.get().jumpPre && !player.onGround && player.motionY < 0.0D && !player.capabilities.isFlying
        && isElytraUpgradeEquipped(player) && !isElytraActive(player)) {
      setActive(player, Type.ELYTRA, true);
      PacketHandler.INSTANCE.sendToServer(new PacketUpgradeState(Type.ELYTRA, true));
    }

    DATA.get().wasJumping = !player.onGround;
    if (!DATA.get().wasJumping) {
      DATA.get().jumpCount = 0;
    }
    DATA.get().ticksSinceLastJump++;
  }

  @SideOnly(Side.CLIENT)
  private static boolean doJump(EntityPlayerSP player) {
    if (!isJumpActive(player)) {
      return false;
    }

    ItemStack boots = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
    JumpUpgrade jumpUpgrade = JumpUpgrade.loadAnyFromItem(boots);

    if (jumpUpgrade == null) {
      return false;
    }

    boolean autoJump = Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.AUTO_JUMP);
    if (autoJump && DATA.get().jumpCount <= 0) {
      DATA.get().jumpCount++;
      return false;
    }

    int autoJumpOffset = autoJump ? 1 : 0;
    int requiredPower = Config.darkSteelBootsJumpPowerCost * (int) Math.pow(DATA.get().jumpCount + 1 - autoJumpOffset, 2.5);
    int availablePower = getPlayerEnergy(player, EntityEquipmentSlot.FEET);
    int maxJumps = jumpUpgrade.getLevel() + autoJumpOffset;
    if (availablePower > 0 && requiredPower <= availablePower && DATA.get().jumpCount < maxJumps) {
      DATA.get().jumpCount++;
      player.motionY += 0.15 * Config.darkSteelBootsJumpModifier * (DATA.get().jumpCount - autoJumpOffset);
      DATA.get().ticksSinceLastJump = 0;

      usePlayerEnergy(player, EntityEquipmentSlot.FEET, requiredPower);
      PacketHandler.INSTANCE.sendToServer(new PacketDarkSteelPowerPacket(requiredPower, EntityEquipmentSlot.FEET));

      jumpUpgrade.doMultiplayerSFX(player);
      PacketHandler.INSTANCE.sendToServer(new PacketDarkSteelSFXPacket(jumpUpgrade, player));

      return true;
    }
    return false;
  }

  private static void updateNightvision(EntityPlayer player) {
    if (isNightVisionUpgradeEquipped(player) && DATA.get().nightVisionActive) {
      player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 210, 0, true, true));
    }
    if (!isNightVisionUpgradeEquipped(player) && DATA.get().nightVisionActive) {
      DATA.get().nightVisionActive = false;
      DATA.get().removeNightvision = true;
    }
    if (DATA.get().removeNightvision) {
      player.removePotionEffect(MobEffects.NIGHT_VISION);
      DATA.get().removeNightvision = false;
    }
  }

  public static boolean isNightVisionUpgradeEquipped(EntityPlayer player) {
    ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
    return NightVisionUpgrade.INSTANCE.hasUpgrade(helmet);
  }

  public static void setNightVisionActive(boolean isNightVisionActive) {
    if (DATA.get().nightVisionActive && !isNightVisionActive) {
      DATA.get().removeNightvision = true;
    }
    DATA.get().nightVisionActive = isNightVisionActive;
  }

  public static boolean isNightVisionActive() {
    return DATA.get().nightVisionActive;
  }

  public static boolean isTopUpgradeEquipped(EntityPlayer player) {
    ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
    return TheOneProbeUpgrade.getInstance().hasUpgrade(helmet);
  }

  public static void setTopActive(EntityPlayer player, boolean active) {
    ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
    if (active) {
      ItemUtil.getOrCreateNBT(helmet).setInteger(TheOneProbeUpgrade.PROBETAG, 1);
    } else {
      ItemUtil.getOrCreateNBT(helmet).removeTag(TheOneProbeUpgrade.PROBETAG);
    }
  }

  public static boolean isTopActive(EntityPlayer player) {
    ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
    return ItemUtil.getOrCreateNBT(helmet).hasKey(TheOneProbeUpgrade.PROBETAG);
  }
}
