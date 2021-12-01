package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;

public class ItemPredicate {
   public static final ItemPredicate ANY = new ItemPredicate();
   @Nullable
   private final Tag<Item> tag;
   @Nullable
   private final Set<Item> items;
   private final MinMaxBounds.Ints count;
   private final MinMaxBounds.Ints durability;
   private final EnchantmentPredicate[] enchantments;
   private final EnchantmentPredicate[] storedEnchantments;
   @Nullable
   private final Potion potion;
   private final NbtPredicate nbt;

   public ItemPredicate() {
      this.tag = null;
      this.items = null;
      this.potion = null;
      this.count = MinMaxBounds.Ints.ANY;
      this.durability = MinMaxBounds.Ints.ANY;
      this.enchantments = EnchantmentPredicate.NONE;
      this.storedEnchantments = EnchantmentPredicate.NONE;
      this.nbt = NbtPredicate.ANY;
   }

   public ItemPredicate(@Nullable Tag<Item> p_151429_, @Nullable Set<Item> p_151430_, MinMaxBounds.Ints p_151431_, MinMaxBounds.Ints p_151432_, EnchantmentPredicate[] p_151433_, EnchantmentPredicate[] p_151434_, @Nullable Potion p_151435_, NbtPredicate p_151436_) {
      this.tag = p_151429_;
      this.items = p_151430_;
      this.count = p_151431_;
      this.durability = p_151432_;
      this.enchantments = p_151433_;
      this.storedEnchantments = p_151434_;
      this.potion = p_151435_;
      this.nbt = p_151436_;
   }

   public boolean matches(ItemStack p_45050_) {
      if (this == ANY) {
         return true;
      } else if (this.tag != null && !p_45050_.is(this.tag)) {
         return false;
      } else if (this.items != null && !this.items.contains(p_45050_.getItem())) {
         return false;
      } else if (!this.count.matches(p_45050_.getCount())) {
         return false;
      } else if (!this.durability.isAny() && !p_45050_.isDamageableItem()) {
         return false;
      } else if (!this.durability.matches(p_45050_.getMaxDamage() - p_45050_.getDamageValue())) {
         return false;
      } else if (!this.nbt.matches(p_45050_)) {
         return false;
      } else {
         if (this.enchantments.length > 0) {
            Map<Enchantment, Integer> map = EnchantmentHelper.deserializeEnchantments(p_45050_.getEnchantmentTags());

            for(EnchantmentPredicate enchantmentpredicate : this.enchantments) {
               if (!enchantmentpredicate.containedIn(map)) {
                  return false;
               }
            }
         }

         if (this.storedEnchantments.length > 0) {
            Map<Enchantment, Integer> map1 = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(p_45050_));

            for(EnchantmentPredicate enchantmentpredicate1 : this.storedEnchantments) {
               if (!enchantmentpredicate1.containedIn(map1)) {
                  return false;
               }
            }
         }

         Potion potion = PotionUtils.getPotion(p_45050_);
         return this.potion == null || this.potion == potion;
      }
   }

   public static ItemPredicate fromJson(@Nullable JsonElement p_45052_) {
      if (p_45052_ != null && !p_45052_.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(p_45052_, "item");
         MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(jsonobject.get("count"));
         MinMaxBounds.Ints minmaxbounds$ints1 = MinMaxBounds.Ints.fromJson(jsonobject.get("durability"));
         if (jsonobject.has("data")) {
            throw new JsonParseException("Disallowed data tag found");
         } else {
            NbtPredicate nbtpredicate = NbtPredicate.fromJson(jsonobject.get("nbt"));
            Set<Item> set = null;
            JsonArray jsonarray = GsonHelper.getAsJsonArray(jsonobject, "items", (JsonArray)null);
            if (jsonarray != null) {
               ImmutableSet.Builder<Item> builder = ImmutableSet.builder();

               for(JsonElement jsonelement : jsonarray) {
                  ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.convertToString(jsonelement, "item"));
                  builder.add(Registry.ITEM.getOptional(resourcelocation).orElseThrow(() -> {
                     return new JsonSyntaxException("Unknown item id '" + resourcelocation + "'");
                  }));
               }

               set = builder.build();
            }

            Tag<Item> tag = null;
            if (jsonobject.has("tag")) {
               ResourceLocation resourcelocation1 = new ResourceLocation(GsonHelper.getAsString(jsonobject, "tag"));
               tag = SerializationTags.getInstance().getTagOrThrow(Registry.ITEM_REGISTRY, resourcelocation1, (p_45054_) -> {
                  return new JsonSyntaxException("Unknown item tag '" + p_45054_ + "'");
               });
            }

            Potion potion = null;
            if (jsonobject.has("potion")) {
               ResourceLocation resourcelocation2 = new ResourceLocation(GsonHelper.getAsString(jsonobject, "potion"));
               potion = Registry.POTION.getOptional(resourcelocation2).orElseThrow(() -> {
                  return new JsonSyntaxException("Unknown potion '" + resourcelocation2 + "'");
               });
            }

            EnchantmentPredicate[] aenchantmentpredicate = EnchantmentPredicate.fromJsonArray(jsonobject.get("enchantments"));
            EnchantmentPredicate[] aenchantmentpredicate1 = EnchantmentPredicate.fromJsonArray(jsonobject.get("stored_enchantments"));
            return new ItemPredicate(tag, set, minmaxbounds$ints, minmaxbounds$ints1, aenchantmentpredicate, aenchantmentpredicate1, potion, nbtpredicate);
         }
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonobject = new JsonObject();
         if (this.items != null) {
            JsonArray jsonarray = new JsonArray();

            for(Item item : this.items) {
               jsonarray.add(Registry.ITEM.getKey(item).toString());
            }

            jsonobject.add("items", jsonarray);
         }

         if (this.tag != null) {
            jsonobject.addProperty("tag", SerializationTags.getInstance().getIdOrThrow(Registry.ITEM_REGISTRY, this.tag, () -> {
               return new IllegalStateException("Unknown item tag");
            }).toString());
         }

         jsonobject.add("count", this.count.serializeToJson());
         jsonobject.add("durability", this.durability.serializeToJson());
         jsonobject.add("nbt", this.nbt.serializeToJson());
         if (this.enchantments.length > 0) {
            JsonArray jsonarray1 = new JsonArray();

            for(EnchantmentPredicate enchantmentpredicate : this.enchantments) {
               jsonarray1.add(enchantmentpredicate.serializeToJson());
            }

            jsonobject.add("enchantments", jsonarray1);
         }

         if (this.storedEnchantments.length > 0) {
            JsonArray jsonarray2 = new JsonArray();

            for(EnchantmentPredicate enchantmentpredicate1 : this.storedEnchantments) {
               jsonarray2.add(enchantmentpredicate1.serializeToJson());
            }

            jsonobject.add("stored_enchantments", jsonarray2);
         }

         if (this.potion != null) {
            jsonobject.addProperty("potion", Registry.POTION.getKey(this.potion).toString());
         }

         return jsonobject;
      }
   }

   public static ItemPredicate[] fromJsonArray(@Nullable JsonElement p_45056_) {
      if (p_45056_ != null && !p_45056_.isJsonNull()) {
         JsonArray jsonarray = GsonHelper.convertToJsonArray(p_45056_, "items");
         ItemPredicate[] aitempredicate = new ItemPredicate[jsonarray.size()];

         for(int i = 0; i < aitempredicate.length; ++i) {
            aitempredicate[i] = fromJson(jsonarray.get(i));
         }

         return aitempredicate;
      } else {
         return new ItemPredicate[0];
      }
   }

   public static class Builder {
      private final List<EnchantmentPredicate> enchantments = Lists.newArrayList();
      private final List<EnchantmentPredicate> storedEnchantments = Lists.newArrayList();
      @Nullable
      private Set<Item> items;
      @Nullable
      private Tag<Item> tag;
      private MinMaxBounds.Ints count = MinMaxBounds.Ints.ANY;
      private MinMaxBounds.Ints durability = MinMaxBounds.Ints.ANY;
      @Nullable
      private Potion potion;
      private NbtPredicate nbt = NbtPredicate.ANY;

      private Builder() {
      }

      public static ItemPredicate.Builder item() {
         return new ItemPredicate.Builder();
      }

      public ItemPredicate.Builder of(ItemLike... p_151446_) {
         this.items = Stream.of(p_151446_).map(ItemLike::asItem).collect(ImmutableSet.toImmutableSet());
         return this;
      }

      public ItemPredicate.Builder of(Tag<Item> p_45070_) {
         this.tag = p_45070_;
         return this;
      }

      public ItemPredicate.Builder withCount(MinMaxBounds.Ints p_151444_) {
         this.count = p_151444_;
         return this;
      }

      public ItemPredicate.Builder hasDurability(MinMaxBounds.Ints p_151450_) {
         this.durability = p_151450_;
         return this;
      }

      public ItemPredicate.Builder isPotion(Potion p_151442_) {
         this.potion = p_151442_;
         return this;
      }

      public ItemPredicate.Builder hasNbt(CompoundTag p_45076_) {
         this.nbt = new NbtPredicate(p_45076_);
         return this;
      }

      public ItemPredicate.Builder hasEnchantment(EnchantmentPredicate p_45072_) {
         this.enchantments.add(p_45072_);
         return this;
      }

      public ItemPredicate.Builder hasStoredEnchantment(EnchantmentPredicate p_151448_) {
         this.storedEnchantments.add(p_151448_);
         return this;
      }

      public ItemPredicate build() {
         return new ItemPredicate(this.tag, this.items, this.count, this.durability, this.enchantments.toArray(EnchantmentPredicate.NONE), this.storedEnchantments.toArray(EnchantmentPredicate.NONE), this.potion, this.nbt);
      }
   }
}