/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps.mixin;

import com.ghosty.nomadscamps.util.TaggedStructureTemplate;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/// An interface mixin that adds additional functionality to Minecraft's base StructureTemplate class.
@Mixin(StructureTemplate.class)
public abstract class TaggedStructureTemplateMixin implements TaggedStructureTemplate {
    // Shadowed fields and methods necessary for the nomads_camps$taggedSaveFromWorld method
    @Shadow
    private final List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = Lists.newArrayList();
    @Shadow
    private final List<StructureTemplate.StructureEntityInfo> entities = Lists.newArrayList();
    @Shadow
    private Vec3i size;

    @Shadow
    private static void categorize(StructureTemplate.StructureBlockInfo blockInfo, List<StructureTemplate.StructureBlockInfo> fullBlocks, List<StructureTemplate.StructureBlockInfo> blocksWithNbt, List<StructureTemplate.StructureBlockInfo> otherBlocks) {
    }

    @Shadow
    private static List<StructureTemplate.StructureBlockInfo> combineSorted(List<StructureTemplate.StructureBlockInfo> fullBlocks, List<StructureTemplate.StructureBlockInfo> blocksWithNbt, List<StructureTemplate.StructureBlockInfo> otherBlocks) {
        return null;
    }

    @Shadow
    private void addEntitiesFromWorld(World world, BlockPos firstCorner, BlockPos secondCorner) {
    }

    /// A modified version of saveFromWorld that can ignore several types of blocks (pulled form a TagKey)
    /// rather than just one.
    ///
    /// @param ignoredBlocks A TagKey of blocks to ignore when saving. Replaces the single block reference
    /// from the base saveFromWorld method.
    @Override
    public void nomads_camps$taggedSaveFromWorld(World world, BlockPos start, Vec3i dimensions, boolean includeEntities, TagKey<Block> ignoredBlocks) {
        if (dimensions.getX() >= 1 && dimensions.getY() >= 1 && dimensions.getZ() >= 1) {
            BlockPos blockPos = start.add(dimensions).add(-1, -1, -1);
            List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
            BlockPos blockPos2 = new BlockPos(Math.min(start.getX(), blockPos.getX()), Math.min(start.getY(), blockPos.getY()), Math.min(start.getZ(), blockPos.getZ()));
            BlockPos blockPos3 = new BlockPos(Math.max(start.getX(), blockPos.getX()), Math.max(start.getY(), blockPos.getY()), Math.max(start.getZ(), blockPos.getZ()));
            this.size = dimensions;

            for (BlockPos blockPos4 : BlockPos.iterate(blockPos2, blockPos3)) {
                BlockPos blockPos5 = blockPos4.subtract(blockPos2);
                BlockState blockState = world.getBlockState(blockPos4);
                // This is the one change between this method and StructureTemplate.saveFromWorld
                if (!blockState.isIn(ignoredBlocks)) {
                    BlockEntity blockEntity = world.getBlockEntity(blockPos4);
                    StructureTemplate.StructureBlockInfo structureBlockInfo;
                    if (blockEntity != null) {
                        structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, blockEntity.createNbtWithId(world.getRegistryManager()));
                    } else {
                        structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, null);
                    }

                    categorize(structureBlockInfo, list, list2, list3);
                }
            }

            List<StructureTemplate.StructureBlockInfo> list4 = combineSorted(list, list2, list3);
            this.blockInfoLists.clear();
            // This is the line that makes PalettedBlockInfoListAccessorMixin necessary.
            this.blockInfoLists.add(PalettedBlockInfoListAccessorMixin.create(list4));
            if (includeEntities) {
                this.addEntitiesFromWorld(world, blockPos2, blockPos3);
            } else {
                this.entities.clear();
            }

        }
    }
}