package crazypants.enderio.machines.config.config;

import com.enderio.core.common.util.NNList;

import crazypants.enderio.base.config.factory.IValue;
import crazypants.enderio.base.config.factory.IValueFactory;
import crazypants.enderio.machines.config.Config;

public class UpgradeConfig {

  public static final IValueFactory F = Config.F.section("upgrades");

  public static final IValueFactory F_SOLAR = F.section(".solar");

  public static final NNList<IValue<Integer>> solarPowerGen = new NNList<>(
      F_SOLAR.make("powerGen1", 10, "Energy per SECOND generated by the Solar I upgrade. Split between all equipped DS armors.").setMin(1).sync(),
      F_SOLAR.make("powerGen2", 40, "Energy per SECOND generated by the Solar II upgrade. Split between all equipped DS armors.").setMin(1).sync(),
      F_SOLAR.make("powerGen3", 160, "Energy per SECOND generated by the Solar III upgrade. Split between all equipped DS armors.").setMin(1).sync());

  public static final NNList<IValue<Integer>> solarUpradeCost = new NNList<>(
      F_SOLAR.make("upgradeCost1", 4, "Cost in XP levels of the Solar I upgrade.").setMin(1).sync(),
      F_SOLAR.make("upgradeCost2", 8, "Cost in XP levels of the Solar II upgrade.").setMin(1).sync(),
      F_SOLAR.make("upgradeCost3", 24, "Cost in XP levels of the Solar III upgrade.").setMin(1).sync());

  public static final IValue<Boolean> helmetChargeOthers = F_SOLAR.make("chargeOthers", true, //
      "If enabled allows the solar upgrade to charge non-darksteel armors that the player is wearing.").sync();
  
  public static final IValueFactory F_WET = F.section(".wet");

  public static final NNList<IValue<Integer>> wetCost = new NNList<IValue<Integer>>(
      F_WET.make("upgradeCost1", 4, "Cost in XP levels of the Wet I upgrade").setMin(1).sync(),
      F_WET.make("upgradeCost2", 6, "Cost in XP levels of the Wet II upgrade").setMin(1).sync(),
      F_WET.make("upgradeCost3", 8, "Cost in XP levels of the Wet III upgrade").setMin(1).sync(),
      F_WET.make("upgradeCost4", 12, "Cost in XP levels of the Wet IV upgrade").setMin(1).sync(),
      F_WET.make("upgradeCost5", 16, "Cost in XP levels of the Wet V upgrade").setMin(1).sync());

  public static final IValue<Float> cobblestoneModifier = F_WET.make("cobblestonePowerMod", 0.2F, //
      "How much power should the upgrade use to convert flowing lava to cobblestone, relative to the power needed to convert still lava to obsidian (0 = 0%, 1 = 100%, can exceed 100%).")
      .setMin(0).sync();

}