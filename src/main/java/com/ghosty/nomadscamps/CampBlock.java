/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/// Defines behavior for the Camp Supplies block (shape, model, item drops, etc.).
public class CampBlock extends BlockWithEntity implements BlockEntityProvider {
    /// The shape of this block for collision and outline purposes. Only used
    /// when this block is facing north or south and sitting on the ground.
    private static final VoxelShape NORTH_SOUTH_SHAPE =
            CampBlock.createCuboidShape(2, 0, 4, 14, 13, 12);
    /// The shape of this block for collision and outline purposes. Only used
    /// when this block is facing east or west and sitting on the ground.
    private static final VoxelShape EAST_WEST_SHAPE =
            CampBlock.createCuboidShape(4, 0, 2, 12, 13, 14);
    /// The shape of this block for collision and outline purposes. Only used
    /// when this block is facing north and hanging on a wall.
    private static final VoxelShape NORTH_MOUNTED_SHAPE =
            CampBlock.createCuboidShape(2, 2, 8, 14, 15, 16);
    /// The shape of this block for collision and outline purposes. Only used
    /// when this block is facing south and hanging on a wall.
    private static final VoxelShape SOUTH_MOUNTED_SHAPE =
            CampBlock.createCuboidShape(2, 2, 0, 14, 15, 8);
    /// The shape of this block for collision and outline purposes. Only used
    /// when this block is facing east and hanging on a wall.
    private static final VoxelShape EAST_MOUNTED_SHAPE =
            CampBlock.createCuboidShape(0, 2, 2, 8, 15, 14);
    /// The shape of this block for collision and outline purposes. Only used
    /// when this block is facing west and hanging on a wall.
    private static final VoxelShape WEST_MOUNTED_SHAPE =
            CampBlock.createCuboidShape(8, 2, 2, 16, 15, 14);


    /// Codec definition for networking, chunk saving.
    public static final MapCodec<CampBlock> CODEC = CampBlock.createCodec(CampBlock::new);

    /// Block state definition to enable rotation of the block.
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    /// Block state definition to enable wall mounting the block.
    public static final BooleanProperty WALL_MOUNTED = Properties.HANGING;

    /// Constructs this class using the method provided by BlockWithEntity,
    /// but also sets a default block state.
    public CampBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(WALL_MOUNTED, false));
    }

    // Some methods that are required by the base game but have no notable logic.

    // region TRIVIAL IMPLEMENTATIONS
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(WALL_MOUNTED);
    }

    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
    // endregion TRIVIAL IMPLEMENTATIONS

    /// Used by the base game to get the outline shape of this block for
    /// collision and rendering purposes. The shape is oriented differently
    /// depending on this block's state.
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if(!state.get(WALL_MOUNTED)) {
            return state.get(FACING).getAxis().equals(Direction.Axis.X) ?
                    EAST_WEST_SHAPE :
                    NORTH_SOUTH_SHAPE;
        }

        return switch(state.get(FACING)) {
            case Direction.SOUTH -> SOUTH_MOUNTED_SHAPE;
            case Direction.EAST -> EAST_MOUNTED_SHAPE;
            case Direction.WEST -> WEST_MOUNTED_SHAPE;
            default -> NORTH_MOUNTED_SHAPE;
        };
    }

    /// Used by the base game to determine a block's state when it is placed.
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction anchoredFace = ctx.getSide();

        // If this was placed on the top or bottom of a block don't
        // wall mount it.
        if(anchoredFace.equals(Direction.UP) || anchoredFace.equals(Direction.DOWN))
            return getDefaultState()
                    .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                    .with(WALL_MOUNTED, false);

        // If this was placed on the side of a block and that block
        // face is a solid full square, wall mount it.
        boolean onSolidWall =  ctx.getWorld().getBlockState(ctx.getBlockPos().offset(anchoredFace.getOpposite()))
                .isSideSolidFullSquare(
                        ctx.getWorld(),
                        ctx.getBlockPos(),
                        anchoredFace);

        return getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(WALL_MOUNTED, onSolidWall);
    }

    /// Used by the base game when a player interacts with this block.
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        //If there isn't a Camp Supplies block entity at the interacted position, use default behavior.
        if (!(world.getBlockEntity(pos) instanceof CampBlockEntity campBlockEntity)) {
            return super.onUse(state, world, pos, player, hit);
        }

        //Otherwise, show the camp supplies GUI to the player that interacted with it.
        campBlockEntity.showGUI(player);
        return ActionResult.SUCCESS;
    }

    /// Used by the base game when this block is destroyed.
    @Override
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (oldState.getBlock() != newState.getBlock() && world.getBlockEntity(pos) instanceof CampBlockEntity campBlockEntity) {
            // If there is a Camp Supplies block entity at the given position, generate an
            // ItemStack for the dropped item and write the supplies' data into that stack.
            ItemStack stack = new ItemStack(this);
            NbtCompound nbt = new NbtCompound();
            campBlockEntity.writeNbt(nbt, world.getRegistryManager());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        // Otherwise, use default behavior.
        super.onStateReplaced(oldState, world, pos, newState, moved);
    }

    /// Used by the base game when this block is placed in the world.
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!world.isClient) {
            if (world.getBlockEntity(pos) instanceof CampBlockEntity campBlockEntity) {
                // If we're running on the server, write the nbt data from the item used
                // into the block entity that was placed.
                NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
                if (component != null) campBlockEntity.readNbt(component.copyNbt(), world.getRegistryManager());
            }
        }
    }
}
