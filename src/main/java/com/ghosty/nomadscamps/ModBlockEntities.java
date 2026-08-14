/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/// Registers mod block entities so they can be recognized by the base game.
public class ModBlockEntities {
    /// The registered camp block entity, to be used by classes in this mod.
    public static final BlockEntityType<CampBlockEntity> CAMP_BLOCK_ENTITY =
            register("camp_supplies", CampBlockEntity::new, ModBlocks.CAMP_SUPPLIES);

    /// Called by the base mod class at startup.
    /// Included here to initialize static members.
    public static void initialize() {
    }

    /// Registers block entities with the base game.
    private static <T extends BlockEntity> BlockEntityType<T> register(String name,
                                                                       BlockEntityType.BlockEntityFactory<? extends T> entityFactory,
                                                                       Block... blocks) {
        Identifier id = Identifier.of(NomadsCamps.MOD_ID, name);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.<T>create(entityFactory, blocks).build());
    }
}