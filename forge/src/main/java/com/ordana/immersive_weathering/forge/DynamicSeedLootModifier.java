package com.ordana.immersive_weathering.forge;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ordana.immersive_weathering.blocks.LeafPileBlock;
import com.ordana.immersive_weathering.integration.DynamicTreesCompat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class DynamicSeedLootModifier extends LootModifier {
   public static final Supplier<Codec<DynamicSeedLootModifier>> CODEC = Suppliers.memoize(
      () -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, DynamicSeedLootModifier::new))
   );
   private static final float SEED_CHANCE = 0.1F;

   public DynamicSeedLootModifier(LootItemCondition[] conditions) {
      super(conditions);
   }

   @NotNull
   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
      BlockState state = (BlockState)context.getParamOrNull(LootContextParams.BLOCK_STATE);
      if (state != null && state.getBlock() instanceof LeafPileBlock) {
         loot.removeIf(DynamicSeedLootModifier::isSaplingOrSeed);
         ItemStack tool = (ItemStack)context.getParamOrNull(LootContextParams.TOOL);
         boolean gentle = tool != null && (tool.is(Items.SHEARS) || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0);
         if (!gentle && DynamicTreesCompat.isLoaded() && context.getRandom().nextFloat() < 0.1F) {
            ItemStack seed = DynamicTreesCompat.seedForTree(((LeafPileBlock)state.getBlock()).getLeafType().getTypeName());
            if (!seed.isEmpty()) {
               loot.add(seed);
            }
         }

         return loot;
      } else {
         return loot;
      }
   }

   private static boolean isSaplingOrSeed(ItemStack stack) {
      if (stack.is(ItemTags.SAPLINGS)) {
         return true;
      } else {
         ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
         return id.getNamespace().startsWith("dynamictrees") && id.getPath().endsWith("_seed");
      }
   }

   public Codec<? extends IGlobalLootModifier> codec() {
      return CODEC.get();
   }
}
