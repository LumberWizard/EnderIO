package crazypants.enderio.base.item.darksteel.upgrade.explosive;

import javax.annotation.Nonnull;

import crazypants.enderio.api.upgrades.IDarkSteelItem;
import crazypants.enderio.api.upgrades.IDarkSteelUpgrade;
import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.config.config.DarkSteelConfig;
import crazypants.enderio.base.handler.darksteel.AbstractUpgrade;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

@EventBusSubscriber(modid = EnderIO.MODID)
public class ExplosiveCarpetUpgrade extends AbstractUpgrade {

  private static final @Nonnull String UPGRADE_NAME = "carpet";

  public static final @Nonnull ExplosiveCarpetUpgrade INSTANCE = new ExplosiveCarpetUpgrade();

  @SubscribeEvent
  public static void registerDarkSteelUpgrades(@Nonnull RegistryEvent.Register<IDarkSteelUpgrade> event) {
    event.getRegistry().register(INSTANCE);
  }

  public ExplosiveCarpetUpgrade() {
    super(UPGRADE_NAME, "enderio.darksteel.upgrade." + UPGRADE_NAME, new ItemStack(Blocks.CARPET, 1, OreDictionary.WILDCARD_VALUE),
        DarkSteelConfig.explosiveCarpetUpgradeCost);
  }

  @Override
  public boolean canAddToItem(@Nonnull ItemStack stack, @Nonnull IDarkSteelItem item) {
    return ExplosiveUpgrade.INSTANCE.hasAnyUpgradeVariant(stack) && !hasUpgrade(stack);
  }

  @Override
  public boolean isUpgradeItem(@Nonnull ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() == getUpgradeItem().getItem() && stack.getCount() == getUpgradeItem().getCount();
  }

}
