package crazypants.enderio.base.integration.jei;

import javax.annotation.Nonnull;

import com.enderio.core.client.handlers.SpecialTooltipHandler;
import com.enderio.core.common.util.NNList;
import com.enderio.core.common.util.NNList.Callback;

import crazypants.enderio.api.IModObject;
import crazypants.enderio.base.init.ModObjectRegistry;
import mezz.jei.api.IModRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

public class DescriptionRecipeCategory {

  public static void register(IModRegistry registry) {
    NNList<ItemStack> items = new NNList<>();
    for (IModObject mo : ModObjectRegistry.getRegistry()) {
      final Item item = mo.getItem();
      if (item != null) {
        final CreativeTabs creativeTab = item.getCreativeTab();
        if (creativeTab != null) {
          item.getSubItems(creativeTab, items);
        }
      }
    }
    items.apply(new Callback<ItemStack>() {
      @Override
      public void apply(@Nonnull ItemStack itemStack) {
        NNList<String> allTooltips = SpecialTooltipHandler.getAllTooltips(itemStack);
        if (!allTooltips.isEmpty()) {
          allTooltips.add(0, TextFormatting.WHITE.toString() + TextFormatting.BOLD + itemStack.getDisplayName() + TextFormatting.RESET);
          allTooltips.add(1, "");
          registry.addIngredientInfo(itemStack, ItemStack.class, allTooltips.toArray(new String[0]));
        }
      }
    });
  }

}
