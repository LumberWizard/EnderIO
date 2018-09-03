package crazypants.enderio.base.integration.jei;

import java.util.Collections;

import javax.annotation.Nonnull;

import com.enderio.core.client.gui.GuiContainerBase;
import com.enderio.core.common.util.NNList;

import crazypants.enderio.base.Log;
import crazypants.enderio.base.config.config.InfinityConfig;
import crazypants.enderio.base.filter.gui.AbstractFilterGui;
import crazypants.enderio.base.gui.GuiContainerBaseEIO;
import crazypants.enderio.base.init.ModObject;
import crazypants.enderio.base.integration.jei.energy.EnergyIngredient;
import crazypants.enderio.base.integration.jei.energy.EnergyIngredientHelper;
import crazypants.enderio.base.integration.jei.energy.EnergyIngredientRenderer;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;

@JEIPlugin
public class JeiPlugin implements IModPlugin {

  private static IJeiRuntime jeiRuntime = null;

  @Override
  public void registerItemSubtypes(@Nonnull ISubtypeRegistry subtypeRegistry) {
    DarkSteelUpgradeRecipeCategory.registerSubtypes(subtypeRegistry);
    MobContainerSubtypeInterpreter.registerSubtypes(subtypeRegistry);
  }

  @Override
  public void registerCategories(@Nonnull IRecipeCategoryRegistration registry) {
    IJeiHelpers jeiHelpers = registry.getJeiHelpers();
    IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

    if (InfinityConfig.inWorldCraftingEnabled.get()) {
      registry.addRecipeCategories(new InfinityRecipeCategory(guiHelper));
    }
  }

  @Override
  public void register(@Nonnull IModRegistry registry) {
    DarkSteelUpgradeRecipeCategory.register(registry);
    DescriptionRecipeCategory.register(registry);
    if (InfinityConfig.inWorldCraftingEnabled.get()) {
      InfinityRecipeCategory.registerExtras(registry);
    }

    registry.addAdvancedGuiHandlers(new AdvancedGuiHandlerEnderIO());
    registry.addGhostIngredientHandler(GuiContainerBaseEIO.class, new GhostIngredientHandlerEnderIO());

    if (!JeiAccessor.ALTERNATIVES.isEmpty()) {
      // These are lookups for the outputs, the real recipes with the same input create a different oredicted variant of the output item.
      registry.addRecipes(JeiAccessor.ALTERNATIVES, VanillaRecipeCategoryUid.CRAFTING);
      Log.debug("Provided " + JeiAccessor.ALTERNATIVES.size() + " synthetic crafting recipes to JEI");
    }

    registry.getJeiHelpers().getIngredientBlacklist().addIngredientToBlacklist(new ItemStack(ModObject.itemEnderface.getItemNN()));

    ItemHidingHelper.hide(registry);
  }

  @Override
  public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntimeIn) {
    JeiPlugin.jeiRuntime = jeiRuntimeIn;
    JeiAccessor.jeiRuntimeAvailable = true;
  }

  public static void setFilterText(@Nonnull String filterText) {
    jeiRuntime.getIngredientFilter().setFilterText(filterText);
  }

  public static @Nonnull String getFilterText() {
    return jeiRuntime.getIngredientFilter().getFilterText();
  }

  public static void showCraftingRecipes() {
    jeiRuntime.getRecipesGui().showCategories(new NNList<>(VanillaRecipeCategoryUid.CRAFTING));
  }

  @Override
  public void registerIngredients(@Nonnull IModIngredientRegistration ingredientRegistration) {
    ingredientRegistration.register(EnergyIngredient.class, Collections.singletonList(new EnergyIngredient()), new EnergyIngredientHelper(),
        EnergyIngredientRenderer.INSTANCE);
  }

}
