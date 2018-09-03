package crazypants.enderio.api.upgrades;

import javax.annotation.Nonnull;

import com.google.common.collect.Multimap;

import crazypants.enderio.base.handler.darksteel.PacketDarkSteelSFXPacket;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * This class is an upgrade that can be applied to Dark Steel items.
 * <p>
 * The upgrade needs to be registered in the {@link net.minecraftforge.event.RegistryEvent.Register}&lt;IDarkSteelUpgrade&gt; event.
 * <p>
 * Upgrades themselves are stateless. They determine if they are present on an item or not. By themselves, upgrades can not do very much---they are ticked and
 * optionally can render on the player. Further functionality needs to be implemented on an event handler that checks for the upgrade's existence.
 * <p>
 * Other interfaces upgrades can implement: IAdvancedTooltipProvider (from endercore), {@link IHasPlayerRenderer}
 * 
 * @author Henry Loenwind
 *
 */
public interface IDarkSteelUpgrade extends IForgeRegistryEntry<IDarkSteelUpgrade> {

  /**
   * @return The unlocalized name to display in the tooltip.
   */
  @Nonnull
  String getUnlocalizedName();

  /**
   * @return The amount of levels it costs to apply this upgrade.
   */
  int getLevelCost();

  /**
   * @return The item that is shown in the JEI recipe for this upgrade.
   */
  @Nonnull
  ItemStack getUpgradeItem();

  /**
   * @return The item name that is shown in the tooltip for available upgrades.
   */
  default @Nonnull String getUpgradeItemName() {
    return getUpgradeItem().getDisplayName();
  }

  /**
   * Checks if the given item can be used to apply this upgrade. This is used in the anvil to select the upgrade.
   * 
   * @param stack
   *          The "right" item in the anvil.
   * @return True if this the the upgrade's recipe item.
   */
  default boolean isUpgradeItem(@Nonnull ItemStack stack) {
    return !stack.isEmpty() && stack.isItemEqual(getUpgradeItem()) && stack.getCount() == getUpgradeItem().getCount();
  }

  /**
   * Checks if the given stack has this upgrade.
   * <p>
   * <em>final</em>
   * 
   * @param stack
   *          An itemstack to test.
   * @return True if the given stack has this upgrade.
   */
  default boolean hasUpgrade(@Nonnull ItemStack stack) {
    return stack.getItem() instanceof IDarkSteelItem ? hasUpgrade(stack, (IDarkSteelItem) stack.getItem()) : false;
  }

  /**
   * Checks if the given stack has this upgrade.
   * 
   * @param stack
   *          An itemstack to test.
   * @param item
   *          The item of the stack, pre-cast to {@link IDarkSteelItem}
   * @return True if the given stack has this upgrade.
   */
  boolean hasUpgrade(@Nonnull ItemStack stack, @Nonnull IDarkSteelItem item);

  /**
   * Checks if this upgrade can be applied to the given item. This is called by the anvil crafting for the "left" item after {@link #isUpgradeItem(ItemStack)}
   * return true for the "right" item.
   * 
   * @param stack
   *          An itemstack to test.
   * @param item
   *          The item of the stack, pre-cast to {@link IDarkSteelItem}
   * @return True if this upgrade can be applied to the given item.
   */
  boolean canAddToItem(@Nonnull ItemStack stack, @Nonnull IDarkSteelItem item);

  /**
   * Applies the upgrade to the item's nbt.
   * 
   * @param stack
   *          The item to upgrade.
   * @param item
   *          The item of the stack, pre-cast to {@link IDarkSteelItem}
   */
  void addToItem(@Nonnull ItemStack stack, @Nonnull IDarkSteelItem item);

  /**
   * See {@link PlayerTickEvent}. Called when the given item is equipped in any {@link EntityEquipmentSlot} and has this upgrade.
   * 
   * @param stack
   *          The stack that has the upgrade.
   * @param item
   *          The item of the stack, pre-cast to {@link IDarkSteelItem}
   * @param player
   *          The player.
   */
  default void onPlayerTick(@Nonnull ItemStack stack, @Nonnull IDarkSteelItem item, @Nonnull EntityPlayer player) {
  }

  /**
   * Called via server when another player activates the SFX for this upgrade. That activation has to send a {@link PacketDarkSteelSFXPacket} to the server if
   * it wants this to fire on other clients, that is not happening auto-magically.
   * 
   * @param otherPlayer
   *          The player that needs SFX.
   */
  @SideOnly(Side.CLIENT)
  default void doMultiplayerSFX(@Nonnull EntityPlayer otherPlayer) {
  }

  default void addAttributeModifiers(@Nonnull EntityEquipmentSlot slot, @Nonnull ItemStack stack, @Nonnull Multimap<String, AttributeModifier> map) {
  }

}
