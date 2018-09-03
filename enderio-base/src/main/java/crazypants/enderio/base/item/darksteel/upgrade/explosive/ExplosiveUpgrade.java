package crazypants.enderio.base.item.darksteel.upgrade.explosive;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.NNList;
import com.google.common.base.Charsets;
import com.google.common.collect.Multimap;

import crazypants.enderio.api.upgrades.IDarkSteelItem;
import crazypants.enderio.api.upgrades.IDarkSteelUpgrade;
import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.config.config.DarkSteelConfig;
import crazypants.enderio.base.handler.darksteel.AbstractUpgrade;
import crazypants.enderio.base.handler.darksteel.PlayerAOEAttributeHandler;
import crazypants.enderio.base.item.darksteel.upgrade.energy.EnergyUpgradeManager;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber(modid = EnderIO.MODID)
public class ExplosiveUpgrade extends AbstractUpgrade {

  private static final @Nonnull String UPGRADE_NAME = "tnt";
  private static final @Nonnull UUID UPGRADE_UUID = UUID.nameUUIDFromBytes(UPGRADE_NAME.getBytes(Charsets.UTF_8));

  public static final @Nonnull ExplosiveUpgrade INSTANCE = new ExplosiveUpgrade(0);
  public static final @Nonnull ExplosiveUpgrade INSTANCE2 = new ExplosiveUpgrade(1);
  public static final @Nonnull ExplosiveUpgrade INSTANCE3 = new ExplosiveUpgrade(2);
  public static final @Nonnull ExplosiveUpgrade INSTANCE4 = new ExplosiveUpgrade(3);
  public static final @Nonnull ExplosiveUpgrade INSTANCE5 = new ExplosiveUpgrade(4);

  @SubscribeEvent
  public static void registerDarkSteelUpgrades(@Nonnull RegistryEvent.Register<IDarkSteelUpgrade> event) {
    event.getRegistry().register(INSTANCE);
    event.getRegistry().register(INSTANCE2);
    event.getRegistry().register(INSTANCE3);
    event.getRegistry().register(INSTANCE4);
    event.getRegistry().register(INSTANCE5);
  }

  public ExplosiveUpgrade(int level) {
    super(UPGRADE_NAME, level, "enderio.darksteel.upgrade." + UPGRADE_NAME + "." + level, new ItemStack(Blocks.TNT),
        DarkSteelConfig.explosiveUpgradeCost.get(level));
  }

  @Override
  public boolean canAddToItem(@Nonnull ItemStack stack, @Nonnull IDarkSteelItem item) {
    return item.isPickaxe() && item.hasUpgradeCallbacks(INSTANCE) && EnergyUpgradeManager.itemHasAnyPowerUpgrade(stack)
        && getUpgradeVariantLevel(stack) == variant - 1;
  }

  static final @Nonnull NNList<AttributeModifier> SIZE = new NNList<>( //
      new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 0, 0), new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 1, 0),
      new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 2, 0), new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 3, 0),
      new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 4, 0), new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 5, 0),
      new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 6, 0), new AttributeModifier(UPGRADE_UUID, UPGRADE_NAME, 7, 0));

  @Override
  public void addAttributeModifiers(@Nonnull EntityEquipmentSlot slot, @Nonnull ItemStack stack, @Nonnull Multimap<String, AttributeModifier> map) {
    if (slot == EntityEquipmentSlot.MAINHAND) {
      boolean depth = ExplosiveDepthUpgrade.INSTANCE.hasUpgrade(stack);
      boolean carpet = ExplosiveCarpetUpgrade.INSTANCE.hasUpgrade(stack);
      final int xz, y, xyz;
      if (carpet && depth) {
        xz = variant;
        y = variant + 2;
        xyz = 0;
      } else if (carpet) {
        xz = variant + 2;
        xyz = y = 0;
      } else if (depth) {
        xz = 1;
        y = variant + 3;
        xyz = 0;
      } else {
        xz = y = 0;
        xyz = variant + 1;
      }

      map.put(PlayerAOEAttributeHandler.AOE_XZ.getName(), SIZE.get(xz));
      map.put(PlayerAOEAttributeHandler.AOE_Y.getName(), SIZE.get(y));
      map.put(PlayerAOEAttributeHandler.AOE_XYZ.getName(), SIZE.get(xyz));
    }
  }

}
