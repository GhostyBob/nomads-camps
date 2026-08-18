package com.ghosty.nomadscamps;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.Objects;

/// Container for all of this mod's custom recipes and recipe serializers
public class ModRecipes {

    /// Called by the main mod class. Registers all the recipes contained in this class.
    public static void initialize() {
        // Register CustomSmithingTransform and its serializer
        Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(NomadsCamps.MOD_ID, CustomSmithingTransform.Serializer.ID),
                CustomSmithingTransform.RegisterSerializer()
        );
        Registry.register(
                Registries.RECIPE_TYPE,
                Identifier.of(NomadsCamps.MOD_ID, CustomSmithingTransform.ID),
                new RecipeType<CustomSmithingTransform>() {
                }
        );
    }

    /// Represents the custom smithing table recipe used to install slot upgrades
    /// in camp supplies.
    public static class CustomSmithingTransform extends SmithingTransformRecipe {
        /// This recipe type's ID. Don't use this in recipe .json files; use the
        /// serializer's ID instead.
        public static final String ID = "custom_smithing_transform_recipe";
        /// A reference to this recipe's serializer.
        public static Serializer SERIALIZER;
        // Fields carried over from SmithingTransformRecipe
        final Ingredient template;
        final Ingredient base;
        final Ingredient addition;
        final ItemStack result;

        /// Essentially a super constructor, but sets the fields again so this
        /// class can access them.
        public CustomSmithingTransform(Ingredient template, Ingredient base, Ingredient addition, ItemStack result) {
            super(template, base, addition, result);
            this.template = template;
            this.base = base;
            this.addition = addition;
            this.result = result;
        }

        /// Called by the base game to determine the recipe's output.
        /// Should not change the game state since it is called when
        /// previewing the output, not when it's actually crafted.
        @Override
        public ItemStack craft(SmithingRecipeInput smithingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
            ItemStack itemStack = smithingRecipeInput.base().copyComponentsToNewStack(this.result.getItem(), this.result.getCount());
            itemStack.applyUnvalidatedChanges(this.result.getComponentChanges());

            // Get the camp supplies nbt compound
            NbtComponent component = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (component != null) {
                NbtCompound nbt = component.copyNbt();
                if (nbt.contains("upgrades")) {
                    // Add an upgrade
                    nbt.putInt("upgrades", nbt.getInt("upgrades") + 1);

                    itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                }
            }

            return itemStack;
        }

        /// Creates the serializer and saves a reference to it in this
        /// class. This method should only be called once by
        /// ModRecipes.initialize().
        ///
        /// @return The newly created serializer.
        public static Serializer RegisterSerializer() {
            SERIALIZER = new Serializer();
            return SERIALIZER;
        }

        /// Returns a reference to this recipe type's serializer.
        /// Overridden so as not to use SmithingTransformRecipe's
        /// serializer by mistake.
        ///
        /// @return A reference to this recipe's serializer.
        @Override
        public RecipeSerializer<?> getSerializer() {
            return SERIALIZER;
        }

        /// The RecipeSerializer responsible for parsing and encoding
        /// CustomSmithingTransform recipes. Identical in all but name
        /// and ID to SmithingTransformRecipe's serializer.
        public static class Serializer implements RecipeSerializer<CustomSmithingTransform> {
            /// The name of this serializer. Use this when creating
            /// .json files that use this recipe.
            private static final String ID = "custom_smithing_transform";

            private static final MapCodec<CustomSmithingTransform> CODEC =
                    RecordCodecBuilder.mapCodec((instance) ->
                            instance.group(Ingredient.ALLOW_EMPTY_CODEC.
                                                    fieldOf("template")
                                                    .forGetter((recipe) -> recipe.template),
                                            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("base")
                                                    .forGetter((recipe) -> recipe.base),
                                            Ingredient.ALLOW_EMPTY_CODEC.fieldOf("addition")
                                                    .forGetter((recipe) -> recipe.addition),
                                            ItemStack.VALIDATED_CODEC.fieldOf("result")
                                                    .forGetter((recipe) -> recipe.result))
                                    .apply(instance, CustomSmithingTransform::new));
            public static final PacketCodec<RegistryByteBuf, CustomSmithingTransform> PACKET_CODEC =
                    PacketCodec.ofStatic(
                            CustomSmithingTransform.Serializer::write,
                            CustomSmithingTransform.Serializer::read);

            public MapCodec<CustomSmithingTransform> codec() {
                return CODEC;
            }

            public PacketCodec<RegistryByteBuf, CustomSmithingTransform> packetCodec() {
                return PACKET_CODEC;
            }

            private static CustomSmithingTransform read(RegistryByteBuf buf) {
                Ingredient ingredient = Ingredient.PACKET_CODEC.decode(buf);
                Ingredient ingredient2 = Ingredient.PACKET_CODEC.decode(buf);
                Ingredient ingredient3 = Ingredient.PACKET_CODEC.decode(buf);
                ItemStack itemStack = ItemStack.PACKET_CODEC.decode(buf);
                return new CustomSmithingTransform(ingredient, ingredient2, ingredient3, itemStack);
            }

            private static void write(RegistryByteBuf buf, CustomSmithingTransform recipe) {
                Ingredient.PACKET_CODEC.encode(buf, recipe.template);
                Ingredient.PACKET_CODEC.encode(buf, recipe.base);
                Ingredient.PACKET_CODEC.encode(buf, recipe.addition);
                ItemStack.PACKET_CODEC.encode(buf, recipe.result);
            }
        }
    }
}
