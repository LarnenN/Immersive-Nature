package com.ordana.immersive_weathering.dynamicpack;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Predicate;
import net.mehvahdjukaar.moonlight.api.resources.RPUtils;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.PaletteColor;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;

public final class IWTextureFallback {
   private IWTextureFallback() {
   }

   public static ResourceLocation find(ResourceManager var0, Block var1, Predicate<String> var2) throws java.io.FileNotFoundException {
      ResourceLocation var3 = null;

      try {
         var3 = RPUtils.findFirstBlockTextureLocation(var0, var1, var2);
         if (exists(var0, var3)) {
            return var3;
         }
      } catch (Throwable var6) {
      }

      try {
         ResourceLocation var4 = RPUtils.findFirstBlockTextureLocation(var0, var1, Predicate.not(var2));
         if (exists(var0, var4)) {
            return var4;
         }
      } catch (Throwable var5) {
      }

      return var3 != null ? var3 : RPUtils.findFirstBlockTextureLocation(var0, var1, var2);
   }

   private static boolean exists(ResourceManager var0, ResourceLocation var1) {
      if (var1 == null) {
         return false;
      } else {
         try {
            ResourceLocation var2 = new ResourceLocation(var1.getNamespace(), "textures/" + var1.getPath() + ".png");
            return var0.getResource(var2).isPresent();
         } catch (Throwable var3) {
            return true;
         }
      }
   }

   public static PaletteColor increaseDownSafe(Palette var0) {
      try {
         return var0.increaseDown();
      } catch (RuntimeException var2) {
         return null;
      }
   }

   public static void removeAlphaSafe(TextureImage var0, int var1) {
      try {
         NativeImage var2 = var0.getImage();
         int var3 = var2.getWidth();
         int var4 = var2.getHeight();
         boolean var5 = false;

         for (int var6 = 0; var6 < var3 && !var5; var6++) {
            for (int var7 = 0; var7 < var4; var7++) {
               int var8 = var2.getPixelRGBA(var6, var7);
               if (var8 >>> 24 != 0 && var8 != var1) {
                  var5 = true;
                  break;
               }
            }
         }

         if (!var5) {
            return;
         }
      } catch (Throwable var9) {
      }

      var0.removeAlpha(var1);
   }
}
