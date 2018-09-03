package crazypants.enderio.conduits.conduit.power;

import javax.annotation.Nonnull;

import com.enderio.core.client.render.IconUtil;

import crazypants.enderio.base.conduit.geom.CollidableComponent;
import crazypants.enderio.conduits.config.ConduitConfig;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static crazypants.enderio.conduits.init.ConduitObject.item_power_conduit;

public final class BasePowerConduitData implements IPowerConduitData {

  private final int id;

  public BasePowerConduitData(int id) {
    this.id = id;
  }

  @Override
  public int getID() {
    return id;
  }

  @Override
  public @Nonnull ItemStack createItemStackForSubtype() {
    return new ItemStack(item_power_conduit.getItemNN(), 1, getID());
  }

  @Override
  public int getMaxEnergyIO() {
    switch (getID()) {
    case 1:
      return ConduitConfig.tier2_maxIO.get();
    case 2:
      return ConduitConfig.tier3_maxIO.get();
    default:
      return ConduitConfig.tier1_maxIO.get();
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public TextureAtlasSprite getTextureForState(@Nonnull CollidableComponent component) {
    if (component.dir == null) {
      return PowerConduit.ICONS.get(PowerConduit.ICON_CORE_KEY + PowerConduit.POSTFIX[getID()]).get(TextureAtlasSprite.class);
    }
    if (PowerConduit.COLOR_CONTROLLER_ID.equals(component.data)) {
      return IconUtil.instance.whiteTexture;
    }
    return PowerConduit.ICONS.get(PowerConduit.ICON_KEY + PowerConduit.POSTFIX[getID()]).get(TextureAtlasSprite.class);
  }

}