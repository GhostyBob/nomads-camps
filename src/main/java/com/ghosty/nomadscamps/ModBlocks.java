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


public class ModBlocks {
    public static final Block CAMP_SUPPLIES = register (
            new CampBlock(AbstractBlock.Settings.create() //Change the settings of the block below
                    .nonOpaque()
            ),
            "camp_supplies",
            new Item.Settings() //Change the settings of the block ITEM below
                    .equipmentSlot((livingEntity, itemStack) -> EquipmentSlot.CHEST)
    );

    public static Block register(Block block, String name, @Nullable Item.Settings blockItemSettings) {
        //Register the block and its item
        Identifier id = Identifier.of(NomadsCamps.MOD_ID, name);

        if(blockItemSettings != null) {
            BlockItem blockItem = new BlockItem(block, blockItemSettings);
            Registry.register(Registries.ITEM, id, blockItem);
        }

        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void initialize() {
        //Registers Camp Supplies as a Functional Block in the creative inventory
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register((itemGroup) -> itemGroup.add(ModBlocks.CAMP_SUPPLIES.asItem()));

        //Sets some parameters for Camp Supplies?
        //CAMP_SUPPLIES.canBeNested() = false;
    }
}
