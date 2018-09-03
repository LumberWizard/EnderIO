package crazypants.enderio.machines.machine.tank;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.api.common.util.ITankAccess;
import com.enderio.core.common.fluid.FluidWrapper;
import com.enderio.core.common.fluid.SmartTank;
import com.enderio.core.common.fluid.SmartTankFluidHandler;
import com.enderio.core.common.util.FluidUtil;
import com.enderio.core.common.util.FluidUtil.FluidAndStackResult;
import com.enderio.core.common.util.ItemUtil;

import crazypants.enderio.base.EnderIO;
import crazypants.enderio.base.fluid.Fluids;
import crazypants.enderio.base.fluid.SmartTankFluidMachineHandler;
import crazypants.enderio.base.integration.actuallyadditions.ActuallyadditionsUtil;
import crazypants.enderio.base.machine.baselegacy.AbstractInventoryMachineEntity;
import crazypants.enderio.base.machine.baselegacy.SlotDefinition;
import crazypants.enderio.base.paint.IPaintable;
import crazypants.enderio.base.sound.SoundHelper;
import crazypants.enderio.base.sound.SoundRegistry;
import crazypants.enderio.base.xp.XpUtil;
import crazypants.enderio.machines.config.config.TankConfig;
import crazypants.enderio.machines.network.PacketHandler;
import crazypants.enderio.util.Prep;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

@Storable
public class TileTank extends AbstractInventoryMachineEntity implements ITankAccess.IExtendedTankAccess, IPaintable.IPaintableTileEntity {

  private static int IO_MB_TICK = 100;

  @Store
  protected final @Nonnull SmartTank tank;
  protected int lastUpdateLevel = -1;

  private boolean tankDirty = false;
  private int lastFluidLuminosity = 0;

  @Store
  private @Nonnull VoidMode voidMode = VoidMode.NEVER;

  public TileTank(EnumTankType tankType) {
    super(new SlotDefinition(0, 2, 3, 4, -1, -1));
    tank = tankType.getTank();
    tank.setTileEntity(this);
  }

  public TileTank() {
    this(EnumTankType.NORMAL);
  }

  @Override
  protected boolean doPush(@Nullable EnumFacing dir) {
    if (super.doPush(dir)) {
      return true;
    }
    if (dir != null && !tank.isEmpty()) {
      if (FluidWrapper.transfer(tank, world, getPos().offset(dir), dir.getOpposite(), IO_MB_TICK) > 0) {
        setTanksDirty();
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean doPull(@Nullable EnumFacing dir) {
    if (super.doPull(dir)) {
      return true;
    }
    if (dir != null && !tank.isFull()) {
      if (FluidWrapper.transfer(world, getPos().offset(dir), dir.getOpposite(), tank, IO_MB_TICK) > 0) {
        setTanksDirty();
        return true;
      }
    }
    return false;
  }

  private int getFilledLevel() {
    int level = (int) Math.floor(16 * tank.getFilledRatio());
    if (level == 0 && tank.getFluidAmount() > 0) {
      level = 1;
    }
    return level;
  }

  public boolean canVoidItems() {
    final FluidStack fluid = tank.getFluid();
    return fluid != null && fluid.getFluid().getTemperature() > 973;
  }

  public boolean canEatXP() {
    final FluidStack fluid = tank.getFluid();
    return fluid == null || (fluid.getFluid() == Fluids.XP_JUICE.getFluid() && tank.getAvailableSpace() >= XpUtil.experienceToLiquid(11));
  }

  public boolean isXpBottle(@Nonnull ItemStack stack) {
    return FluidUtil.getFluidTypeFromItem(stack) == null && (isVanillaXpBottle(stack) || ActuallyadditionsUtil.isAAXpBottle(stack));
  }

  private boolean isVanillaXpBottle(@Nonnull ItemStack stack) {
    return stack.getItem() == Items.EXPERIENCE_BOTTLE;
  }

  private int getXpFromBottle(@Nonnull ItemStack stack) {
    if (isVanillaXpBottle(stack)) {
      return 3 + world.rand.nextInt(5) + world.rand.nextInt(5);
    } else if (ActuallyadditionsUtil.isAAXpBottle(stack)) {
      return ActuallyadditionsUtil.getXpFromBottle(stack);
    } else {
      return 0;
    }
  }

  public @Nonnull VoidMode getVoidMode() {
    return voidMode;
  }

  public void setVoidMode(@Nonnull VoidMode mode) {
    this.voidMode = mode;
  }

  @Override
  public boolean isMachineItemValidForSlot(int i, @Nonnull ItemStack item) {
    if (canVoidItems() && voidMode == VoidMode.ALWAYS && i < 2) {
      return false;
    }
    if (i == 0) {
      return (FluidUtil.getFluidTypeFromItem(item) != null) || (isXpBottle(item) && canEatXP());
    } else if (i == 1) {
      return FluidUtil.hasEmptyCapacity(item) || canBeMended(item);
    } else if (i == 2 && canVoidItems()) {
      return voidMode == VoidMode.ALWAYS || (voidMode == VoidMode.NEVER ? false : !FluidUtil.isFluidContainer(item));
    }
    return false;
  }

  @Override
  public boolean isActive() {
    return false;
  }

  private static long lastSendTickAll = -1;
  private long nextSendTickThis = -1;
  private int sendPrio = 0;

  /*
   * Limit sending of client updates because a group of tanks pushing into each other can severely kill the clients fps.
   */
  private boolean canSendClientUpdate() {
    long tick = EnderIO.proxy.getServerTickCount();
    if (nextSendTickThis > tick) {
      return false;
    }
    if (tick == lastSendTickAll && sendPrio++ < 200) {
      return false;
    }
    nextSendTickThis = (lastSendTickAll = tick) + 10 + sendPrio * 2;
    sendPrio = 0;
    return true;
  }

  @Override
  protected boolean processTasks(boolean redstoneCheck) {
    processItems(redstoneCheck);
    int filledLevel = getFilledLevel();
    if (lastUpdateLevel != filledLevel) {
      lastUpdateLevel = filledLevel;
      setTanksDirty();
    }
    if (tankDirty && canSendClientUpdate()) {
      PacketHandler.sendToAllAround(new PacketTankFluid(this), this);
      world.updateComparatorOutputLevel(pos, getBlockType());
      updateLight();
      tankDirty = false;
    }
    return false;
  }

  public void updateLight() {
    final FluidStack fluid = tank.getFluid();
    int thisFluidLuminosity = fluid == null || fluid.getFluid() == null || tank.isEmpty() ? 0 : fluid.getFluid().getLuminosity(fluid);
    if (thisFluidLuminosity != lastFluidLuminosity) {
      if (world.checkLightFor(EnumSkyBlock.BLOCK, getPos())) {
        updateBlock();
      }
      lastFluidLuminosity = thisFluidLuminosity;
    }
  }

  public int getComparatorOutput() {
    if (tank.isEmpty()) {
      return 0;
    }

    return (int) (1 + ((double) tank.getFluidAmount() / (double) tank.getCapacity()) * 14);
  }

  private boolean processItems(boolean redstoneCheck) {
    if (!redstoneCheck) {
      return false;
    }
    if (!shouldDoWorkThisTick(getBlockMetadata() > 0 ? 10 : 20)) {
      return false;
    }
    final ItemStack stack = getStackInSlot(2);
    if (Prep.isValid(stack) && canVoidItems()) {
      if (TankConfig.tankSmeltTrashIntoLava.get() && !tank.isFull() && tank.hasFluid(FluidRegistry.LAVA) && stack.getItem() instanceof ItemBlock) {
        tank.addFluidAmount((int) MathHelper.clamp(world.rand.nextGaussian() * .75 + 3.5, 1, 10)); // 49% for 3, 22%: for 2 and 4, 2.2% for 1 and 5
        stack.shrink(1);
      } else {
        stack.shrink(10);
      }
      SoundHelper.playSound(world, pos, SoundHelper.BLOCK_CENTER, SoundRegistry.ITEM_BURN, 0.05F, 2.0F + world.rand.nextFloat() * 0.4F);
      markDirty();
    }
    return drainFullContainer() || fillEmptyContainer() || mendItem();
  }

  private boolean canBeMended(@Nonnull ItemStack stack) {
    return Prep.isValid(stack) && stack.isItemDamaged() && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack) > 0
        && tank.hasFluid(Fluids.XP_JUICE.getFluid());
  }

  private boolean mendItem() {
    final int output = getSlotDefinition().getMaxOutputSlot();
    final int input = getSlotDefinition().getMinInputSlot() + 1;
    if (tank.isEmpty() || !canBeMended(getStackInSlot(input)) || Prep.isValid(getStackInSlot(output))) {
      return false;
    }

    int damageMendable = Math.min(xpToDurability(XpUtil.liquidToExperience(tank.getFluidAmount())), getStackInSlot(input).getItemDamage());
    if (damageMendable < 1) {
      return false;
    }
    getStackInSlot(input).setItemDamage(inventory[input].getItemDamage() - damageMendable);
    tank.drainInternal(XpUtil.experienceToLiquid(durabilityToXp(damageMendable)), true);

    if (!getStackInSlot(input).isItemDamaged()) {
      setInventorySlotContents(output, getStackInSlot(input));
      setInventorySlotContents(input, Prep.getEmpty());
    }

    markDirty();

    return true;
  }

  public static int durabilityToXp(int durability) {
    return durability / 2;
  }

  public static int xpToDurability(int xp) {
    return xp * 2;
  }

  private boolean fillEmptyContainer() {
    final int input = getSlotDefinition().getMinInputSlot() + 1;
    final ItemStack inputStack = getStackInSlot(input);
    if (Prep.isInvalid(inputStack) || tank.isEmpty()) {
      return false;
    }

    final FluidTank outputTank = getOutputTanks()[0];
    final FluidAndStackResult fill = FluidUtil.tryFillContainer(inputStack, outputTank.getFluid());
    if (fill.result.fluidStack == null) {
      return false;
    }

    final int output = getSlotDefinition().getMaxOutputSlot();
    final ItemStack outputStack = getStackInSlot(output);

    if (Prep.isValid(outputStack) && Prep.isValid(fill.result.itemStack)) {
      if (outputStack.isStackable() && ItemUtil.areStackMergable(outputStack, fill.result.itemStack)
          && (fill.result.itemStack.getCount() + outputStack.getCount()) <= outputStack.getMaxStackSize()) {
        fill.result.itemStack.grow(outputStack.getCount());
      } else {
        return false;
      }
    }

    outputTank.setFluid(fill.remainder.fluidStack);
    setInventorySlotContents(input, fill.remainder.itemStack);
    setInventorySlotContents(output, fill.result.itemStack);

    setTanksDirty();
    markDirty();
    return false;
  }

  private boolean drainFullContainer() {
    final int input = getSlotDefinition().getMinInputSlot();
    final ItemStack inputStack = getStackInSlot(input);
    if (Prep.isInvalid(inputStack) || tank.isFull()) {
      return false;
    }

    final FluidAndStackResult fill = FluidUtil.tryDrainContainer(inputStack, this);
    if (fill.result.fluidStack == null) {
      if (isXpBottle(inputStack) && canEatXP()) {
        if (tank.fill(new FluidStack(Fluids.XP_JUICE.getFluid(), XpUtil.experienceToLiquid(getXpFromBottle(inputStack))), true) > 0) {
          inputStack.shrink(1);
          setInventorySlotContents(input, inputStack);
          setTanksDirty();
          markDirty();
          // The event takes a blockpos and plays at that corner. At least vary the corner a bit...
          BlockPos eventPos = pos;
          if (world.rand.nextBoolean()) {
            eventPos = eventPos.south();
          }
          if (world.rand.nextBoolean()) {
            eventPos = eventPos.east();
          }
          world.playEvent(2002, eventPos, PotionUtils.getPotionColor(PotionTypes.WATER));
        }
      }
      return false;
    }

    final int output = getSlotDefinition().getMinOutputSlot();
    final ItemStack outputStack = getStackInSlot(output);

    if (Prep.isValid(outputStack) && Prep.isValid(fill.result.itemStack)) {
      if (outputStack.isStackable() && ItemUtil.areStackMergable(outputStack, fill.result.itemStack)
          && (fill.result.itemStack.getCount() + outputStack.getCount()) <= outputStack.getMaxStackSize()) {
        fill.result.itemStack.grow(outputStack.getCount());
      } else {
        return false;
      }
    }

    getInputTank(fill.result.fluidStack).setFluid(fill.remainder.fluidStack);
    setInventorySlotContents(input, fill.remainder.itemStack);
    if (Prep.isValid(fill.result.itemStack)) {
      setInventorySlotContents(output, fill.result.itemStack);
    }

    setTanksDirty();
    markDirty();
    return false;
  }

  @Override
  public FluidTank getInputTank(FluidStack forFluidType) {
    return tank;
  }

  @Override
  public @Nonnull FluidTank[] getOutputTanks() {
    return new FluidTank[] { tank };
  }

  @Override
  public void setTanksDirty() {
    if (!tankDirty) {
      tankDirty = true;
      markDirty();
    }
  }

  @Override
  public boolean shouldRenderInPass(int pass) {
    return pass == 1 && tank.getFluidAmount() > 0;
  }

  @SuppressWarnings("null")
  @Override
  @Nonnull
  public List<ITankData> getTankDisplayData() {
    return Collections.<ITankData> singletonList(new ITankData() {

      @Override
      @Nonnull
      public EnumTankType getTankType() {
        return EnumTankType.STORAGE;
      }

      @Override
      @Nullable
      public FluidStack getContent() {
        return tank.getFluid();
      }

      @Override
      public int getCapacity() {
        return tank.getCapacity();
      }
    });
  }

  private SmartTankFluidHandler smartTankFluidHandler;

  protected SmartTankFluidHandler getSmartTankFluidHandler() {
    if (smartTankFluidHandler == null) {
      smartTankFluidHandler = new SmartTankFluidMachineHandler(this, tank);
    }
    return smartTankFluidHandler;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facingIn) {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
      return (T) getSmartTankFluidHandler().get(facingIn);
    }
    return super.getCapability(capability, facingIn);
  }

}
