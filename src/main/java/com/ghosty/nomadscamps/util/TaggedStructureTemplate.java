package com.ghosty.nomadscamps.util;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public interface TaggedStructureTemplate {
    default void nomads_camps$taggedSaveFromWorld(World world, BlockPos start, Vec3i dimensions, boolean includeEntities, TagKey<Block> ignoredBlocks) {
        throw new AssertionError("Method is implemented in a mixin.");
    }
}
