package com.ghosty.nomadscamps;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CampBlock extends BlockWithEntity implements BlockEntityProvider {
    private static final VoxelShape SHAPE =
            CampBlock.createCuboidShape(2, 0, 2, 14, 13, 14);

    public static final MapCodec<CampBlock> CODEC = CampBlock.createCodec(CampBlock::new);

    public CampBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CampBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        //If there isn't a camp supplies block enetity at the interacted position, do the default thing
        if(!(world.getBlockEntity(pos) instanceof CampBlockEntity campBlockEntity)) {
            return super.onUse(state, world, pos, player, hit);
        }

        //Else, do the camp supplies specific thing
        campBlockEntity.showGUI(player);

        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean moved) {
        if(/*what is this first check???*/ oldState.getBlock() != newState.getBlock() && world.getBlockEntity(pos) instanceof CampBlockEntity campBlockEntity) {
            ItemStack stack = new ItemStack(this);
            //Writes campBlockEntity's uuid into nbt
            NbtCompound nbt = new NbtCompound();
            campBlockEntity.writeNbt(nbt, world.getRegistryManager());
            stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(nbt));

            ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        super.onStateReplaced(oldState, world, pos, newState, moved);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if(!world.isClient) {
            if(world.getBlockEntity(pos) instanceof CampBlockEntity campBlockEntity) {
                NbtComponent component = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
                if(component != null) campBlockEntity.readNbt(component.copyNbt(), world.getRegistryManager());
            }
        }
    }
}
