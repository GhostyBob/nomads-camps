/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;

/// Registers mod blocks so they can be recognized by the base game.
public class ModBlocks {
    /// The registered camp block, to be used by classes in this mod.
    public static final Block CAMP_SUPPLIES = register(
            new CampBlock(AbstractBlock.Settings.create() // Settings for the block
                    .nonOpaque()
            ),
            "camp_supplies",
            new Item.Settings() // Settings for the block item
                    .equipmentSlot((livingEntity, itemStack) -> EquipmentSlot.CHEST)
    );

    /// Called by the base mod class on startup. Included for static
    /// initialization and some settings tweaks.
    public static void initialize() {
        // Registers Camp Supplies as a Functional Block in the creative inventory.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register((itemGroup) -> itemGroup.add(ModBlocks.CAMP_SUPPLIES.asItem()));
    }

    /// Registers blocks and their corresponding items with the base game.
    public static Block register(Block block, String name, @Nullable Item.Settings blockItemSettings) {
        Identifier id = Identifier.of(NomadsCamps.MOD_ID, name);

        if (blockItemSettings != null) {
            BlockItem blockItem = new BlockItem(block, blockItemSettings);
            Registry.register(Registries.ITEM, id, blockItem);
        }

        return Registry.register(Registries.BLOCK, id, block);
    }
}
