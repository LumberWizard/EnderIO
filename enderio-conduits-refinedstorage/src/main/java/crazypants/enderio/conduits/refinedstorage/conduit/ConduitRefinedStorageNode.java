package crazypants.enderio.conduits.refinedstorage.conduit;

import java.util.LinkedList;
import java.util.Queue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.network.INetworkNodeVisitor;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.api.storage.IStorageProvider;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.api.util.IComparer;

import crazypants.enderio.base.conduit.ConnectionMode;
import crazypants.enderio.base.conduit.item.FunctionUpgrade;
import crazypants.enderio.base.conduit.item.ItemFunctionUpgrade;
import crazypants.enderio.base.filter.IFilter;
import crazypants.enderio.base.filter.fluid.IFluidFilter;
import crazypants.enderio.base.filter.item.IItemFilter;
import crazypants.enderio.conduits.refinedstorage.RSHelper;
import crazypants.enderio.conduits.refinedstorage.init.ConduitRefinedStorageObject;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class ConduitRefinedStorageNode implements INetworkNode, INetworkNodeVisitor {

  public static final @Nonnull String ID = "rs_conduit";

  @Nullable protected INetwork rsNetwork;
  protected @Nonnull World world;
  protected @Nonnull BlockPos pos;
  protected @Nonnull IRefinedStorageConduit con;
  protected int compare = IComparer.COMPARE_DAMAGE;

  private int tickCount = 0;
  private int itemsPerTick = 4;
  private @Nonnull Queue<EnumFacing> dirsToCheck;

  private int currentSlot;

  private int importFilterSlot;
  private int exportFilterSlot;

  private int slotsToCheck;

  public ConduitRefinedStorageNode(@Nonnull IRefinedStorageConduit con) {
    this.con = con;
    this.world = con.getBundle().getBundleworld();
    this.pos = con.getBundle().getLocation();

    dirsToCheck = new LinkedList<>();

    for (EnumFacing dir : EnumFacing.values()) {
      dirsToCheck.offer(dir);
    }
  }

  @Override
  public int getEnergyUsage() {
    return 0;
  }

  @Nonnull
  @Override
  public ItemStack getItemStack() {
    return new ItemStack(ConduitRefinedStorageObject.item_refined_storage_conduit.getItemNN(), 1);
  }

  @Override
  public void onConnected(INetwork network) {
    rsNetwork = network;
  }

  @Override
  public void onDisconnected(INetwork network) {
    rsNetwork = null;
  }

  @Override
  public boolean canUpdate() {
    return con.hasExternalConnections();
  }

  @Nullable
  @Override
  public INetwork getNetwork() {
    return rsNetwork;
  }

  @Override
  public void update() {
    if (canUpdate()) {
      tickCount++;
      if (rsNetwork != null && tickCount > 4) {
        tickCount = 0;

        for (int i = 0; i < dirsToCheck.size(); i++) {
          EnumFacing dir = dirsToCheck.poll();
          dirsToCheck.offer(dir);

          if (con.containsExternalConnection(dir) && updateDir(dir)) {
            break;
          }
        }
      }
    }
  }

  private boolean updateDir(@Nonnull EnumFacing dir) {
    TileEntity te = world.getTileEntity(pos.offset(dir));

    boolean done = false;

    if (te != null && !(te instanceof IStorageProvider)) {
      done = updateDirItems(dir, te);
      done = done || updateDirFluids(dir, te);
    }

    return done;
  }

  private boolean updateDirFluids(@Nonnull EnumFacing dir, @Nonnull TileEntity te) {
    if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite())) {

      IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite());

      if (handler != null) {

        // Export
        IFilter outFilt = con.getOutputFilter(dir);
        if (outFilt instanceof IFluidFilter) {

          IFluidFilter exportFilter = (IFluidFilter) outFilt;

          FluidStack stack = null;

          if (!exportFilter.isEmpty()) {
            do {
              stack = exportFilter.getFluidStackAt(exportFilterSlot >= exportFilter.getSlotCount() ? exportFilterSlot = 0 : exportFilterSlot);

              if (stack == null) {
                exportFilterSlot++;
              }
            } while (stack == null);
          }

          ItemStack upgrade = con.getUpgradeStack(dir.ordinal());
          FunctionUpgrade up = null;

          if (!upgrade.isEmpty()) {
            up = ((ItemFunctionUpgrade) upgrade.getItem()).getFunctionUpgrade();
          }

          if (stack != null) {
            int toExtract = Fluid.BUCKET_VOLUME;

            FluidStack stackInStorage = rsNetwork.getFluidStorageCache().getList().get(stack, compare);

            if (stackInStorage != null) {
              toExtract = Math.min(toExtract, stackInStorage.amount);

              FluidStack took = rsNetwork.extractFluid(stack, toExtract, compare, Action.SIMULATE);

              if (took != null) {
                int filled = handler.fill(took, false);

                if (filled > 0) {
                  took = rsNetwork.extractFluid(stack, filled, compare, Action.PERFORM);

                  handler.fill(took, true);
                  exportFilterSlot++;
                  return true;
                }
              }
            } else if (up != null && isCraftingUpgrade(up)) {
              rsNetwork.getCraftingManager().request(stack, toExtract);
            }
          }
        }

        // Importing
        IFilter inFilt = con.getInputFilter(dir);
        if (inFilt instanceof IFluidFilter) {

          IFluidFilter importFilter = (IFluidFilter) inFilt;

          boolean all = importFilter.isEmpty();

          FluidStack toDrain = handler.drain(Fluid.BUCKET_VOLUME, false);

          FluidStack stack = null;

          if (!all) {
            do {
              stack = importFilter.getFluidStackAt(importFilterSlot >= importFilter.getSlotCount() ? importFilterSlot = 0 : importFilterSlot);

              if (stack == null) {
                importFilterSlot++;
              }
            } while (stack == null);
          }

          if (all || (stack != null && toDrain != null && stack.isFluidEqual(toDrain))) {

            if (toDrain != null) {
              FluidStack remainder = rsNetwork.insertFluidTracked(toDrain, toDrain.amount);
              if (remainder != null) {
                toDrain.amount -= remainder.amount;
              }

              handler.drain(toDrain, true);
              importFilterSlot++;
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private boolean updateDirItems(@Nonnull EnumFacing dir, @Nonnull TileEntity te) {
    if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {

      IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());

      if (handler != null) {

        ItemStack upgrade = con.getUpgradeStack(dir.ordinal());
        FunctionUpgrade up = null;

        if (!upgrade.isEmpty()) {
          up = ((ItemFunctionUpgrade) upgrade.getItem()).getFunctionUpgrade();
          itemsPerTick = up.getMaximumExtracted(64);
        } else {
          itemsPerTick = 4;
        }

        // Exporting
        IFilter outFilt = con.getOutputFilter(dir);
        if (outFilt instanceof IItemFilter) {

          IItemFilter exportFilter = (IItemFilter) outFilt;

          ItemStack slot = ItemStack.EMPTY;

          if (!exportFilter.isEmpty()) {
            do {
              slot = exportFilter.getInventorySlotContents(exportFilterSlot >= exportFilter.getSlotCount() ? exportFilterSlot = 0 : exportFilterSlot);

              if (slot.isEmpty()) {
                exportFilterSlot++;
              }
            } while (slot.isEmpty());
          }

          if (!slot.isEmpty()) {
            ItemStack took = rsNetwork.extractItem(slot, Math.min(slot.getMaxStackSize(), itemsPerTick), compare, Action.SIMULATE);

            if (took == null) {
              if (up != null && isCraftingUpgrade(up)) {
                rsNetwork.getCraftingManager().request(slot, itemsPerTick);
              } else {
                exportFilterSlot++;
              }
              return false;
            } else if (ItemHandlerHelper.insertItem(handler, took, true).isEmpty()) {
              took = rsNetwork.extractItem(slot, Math.min(slot.getMaxStackSize(), itemsPerTick), compare, Action.PERFORM);

              if (took != null) {
                ItemHandlerHelper.insertItem(handler, took, false);
                exportFilterSlot++;
                return true;
              }
            }
          }
        }

        // Importing
        IFilter inFilt = con.getInputFilter(dir);
        if (inFilt instanceof IItemFilter) {

          IItemFilter importFilter = (IItemFilter) inFilt;

          boolean all = importFilter.isEmpty();

          ItemStack slot = ItemStack.EMPTY;

          if (!all) {
            do {
              slot = importFilter.getInventorySlotContents(importFilterSlot >= importFilter.getSlotCount() ? importFilterSlot = 0 : importFilterSlot);

              if (slot.isEmpty()) {
                importFilterSlot++;
                slotsToCheck = 0;
              }
            } while (slot.isEmpty());
          }

          if (all || !slot.isEmpty()) {

            if (currentSlot >= handler.getSlots()) {
              currentSlot = 0;
            }

            if (handler.getSlots() > 0) {
              while (currentSlot + 1 < handler.getSlots() && (handler.getStackInSlot(currentSlot).isEmpty() || (!all && !handler.getStackInSlot(currentSlot)
                  .isItemEqual(importFilter.getInventorySlotContents(importFilterSlot))))) {
                currentSlot++;
                slotsToCheck++;

                if (slotsToCheck >= handler.getSlots()) {
                  importFilterSlot++;
                }
              }

              ItemStack stack = handler.getStackInSlot(currentSlot);

              ItemStack result = handler.extractItem(currentSlot, itemsPerTick, true);

              if (!result.isEmpty() && rsNetwork.insertItem(result, result.getCount(), Action.SIMULATE) == null) {
                result = handler.extractItem(currentSlot, itemsPerTick, false);

                if (!result.isEmpty()) {
                  rsNetwork.insertItemTracked(result, result.getCount());
                }
              } else {
                currentSlot++;
              }
            }
          }
        }
      }
    }
    return false;
  }

  private boolean isCraftingUpgrade(@Nonnull FunctionUpgrade functionUpgrade) {
    return functionUpgrade == FunctionUpgrade.RS_CRAFTING_UPGRADE || functionUpgrade == FunctionUpgrade.RS_CRAFTING_SPEED_UPGRADE
        || functionUpgrade == FunctionUpgrade.RS_CRAFTING_SPEED_DOWNGRADE;
  }

  @Override
  public NBTTagCompound write(NBTTagCompound tag) {
    return tag;
  }

  @Override
  public BlockPos getPos() {
    return pos;
  }

  @Override
  public World getWorld() {
    return world;
  }

  @Override
  public void markDirty() {
    if (!world.isRemote) {
      RSHelper.API.getNetworkNodeManager(world).markForSaving();
    }
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean equals(Object right) {
    return RSHelper.API.isNetworkNodeEqual(this, right);
  }

  @Override
  public int hashCode() {
    return RSHelper.API.getNetworkNodeHashCode(this);
  }

  public boolean canConduct(@Nonnull EnumFacing direction) {
    return con.containsConduitConnection(direction) || con.getConnectionMode(direction) != ConnectionMode.DISABLED;
  }

  @Override
  public void visit(Operator operator) {
    for (EnumFacing facing : EnumFacing.VALUES) {
      if (canConduct(facing)) {
        operator.apply(world, pos.offset(facing), facing.getOpposite());
      }
    }
  }

  public void onConduitConnectionChange() {
    if (rsNetwork != null) {
      rsNetwork.getNodeGraph().rebuild();
    }
  }

}
