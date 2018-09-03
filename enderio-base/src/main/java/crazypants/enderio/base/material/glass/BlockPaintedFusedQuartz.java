package crazypants.enderio.base.material.glass;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.NNList;
import com.enderio.core.common.util.NNList.NNIterator;

import crazypants.enderio.base.block.painted.BlockItemPaintedBlock.INamedSubBlocks;
import crazypants.enderio.api.IModObject;
import crazypants.enderio.base.block.painted.TileEntityPaintedBlock;
import crazypants.enderio.base.paint.IPaintable;
import crazypants.enderio.base.paint.PaintUtil;
import crazypants.enderio.base.paint.render.PaintHelper;
import crazypants.enderio.base.recipe.MachineRecipeRegistry;
import crazypants.enderio.base.render.IBlockStateWrapper;
import crazypants.enderio.base.render.ICustomSubItems;
import crazypants.enderio.base.render.IRenderMapper.IItemRenderMapper;
import crazypants.enderio.base.render.pipeline.BlockStateWrapperBase;
import crazypants.enderio.base.render.registry.SmartModelAttacher;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockPaintedFusedQuartz extends BlockFusedQuartzBase<TileEntityPaintedBlock>
    implements ITileEntityProvider, IPaintable.IBlockPaintableBlock, INamedSubBlocks, ICustomSubItems {

  public static BlockPaintedFusedQuartz create(@Nonnull IModObject modObject) {
    BlockPaintedFusedQuartz result = new BlockPaintedFusedQuartz(modObject);
    result.init();
    return result;
  }

  @SuppressWarnings("null")
  private BlockPaintedFusedQuartz(@Nonnull IModObject modObject) {
    super(modObject);
    setCreativeTab(null);
    setDefaultState(getBlockState().getBaseState().withProperty(FusedQuartzType.KIND, FusedQuartzType.FUSED_QUARTZ));
  }

  @Override
  public @Nonnull String getUnlocalizedName(int meta) {
    return FusedQuartzType.getTypeFromMeta(meta).getBlock().getUnlocalizedName();
  }

  @Override
  protected void init() {
    super.init();
    SmartModelAttacher.registerNoProps(this);
    NNList<Block> blocks = new NNList<Block>();
    NNIterator<FusedQuartzType> iterator = NNList.of(FusedQuartzType.class).iterator();
    while (iterator.hasNext()) {
      blocks.add(iterator.next().getBlock());
    }
    MachineRecipeRegistry.instance.registerRecipe(MachineRecipeRegistry.PAINTER, new GlassPaintingRecipe(this, blocks.toArray(new Block[0])));
  }

  @Override
  protected @Nonnull BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, new IProperty[] { FusedQuartzType.KIND });
  }

  @Override
  @SideOnly(Side.CLIENT)
  public @Nonnull IBlockState getExtendedState(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
    IBlockStateWrapper blockStateWrapper = new BlockStateWrapperBase(state, world, pos, null);
    blockStateWrapper.addCacheKey(0);
    blockStateWrapper.bakeModel();
    return blockStateWrapper;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public @Nonnull IItemRenderMapper getItemRenderMapper() {
    // this should never be called as this block's item will always be painted
    return new IItemRenderMapper() {
    };
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void getSubBlocks(@Nonnull CreativeTabs par2CreativeTabs, @Nonnull NonNullList<ItemStack> par3List) {
    // Painted blocks don't show in the Creative Inventory or JEI
  }

  @Override
  @Nonnull
  public NNList<ItemStack> getSubItems() {
    return getSubItems(this, 0, FusedQuartzType.values().length - 1);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public float getAmbientOcclusionLightValue(@Nonnull IBlockState bs) {
    return 1;
  }

  @Override
  public boolean doesSideBlockRendering(@Nonnull IBlockState bs, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return false;
  }

  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int metadata) {
    return new TileEntityPaintedBlock();
  }

  @Override
  public @Nonnull ItemStack getPickBlock(@Nonnull IBlockState bs, @Nonnull RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull EntityPlayer player) {
    final ItemStack pickBlock = super.getPickBlock(bs, target, world, pos, player);
    PaintUtil.setSourceBlock(pickBlock, getPaintSource(world.getBlockState(pos), world, pos));
    return pickBlock;
  }

  @Override
  public boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
    return true;
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean addHitEffects(@Nonnull IBlockState state, @Nonnull World world, @Nonnull RayTraceResult target, @Nonnull ParticleManager effectRenderer) {
    return PaintHelper.addHitEffects(state, world, target, effectRenderer);
  }

  @SideOnly(Side.CLIENT)
  @Override
  public boolean addDestroyEffects(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull ParticleManager effectRenderer) {
    return PaintHelper.addDestroyEffects(world, pos, effectRenderer);
  }

}
