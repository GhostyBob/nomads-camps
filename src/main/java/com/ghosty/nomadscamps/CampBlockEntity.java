package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.ReturnSlotsPayload;
import com.ghosty.nomadscamps.networking.ShowGUIPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class CampBlockEntity extends BlockEntity {

    // region FIELDS
    private BlockPos pos;

    // endregion FIELDS

    //Constructor
    public CampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMP_BLOCK_ENTITY, pos, state);
        this.pos = pos;
        //structureSlots = new ArrayList<>(numStructureSlots);
    }

    // region OWNERSHIP

    //private final UUID DEFAULTUUID = UUID.fromString("ab63890e-685f-4ccd-b319-1b62d2d39444");
    private UUID uuid = null;
    private String ownerName = null;


    public void showGUI(PlayerEntity player) {
        //Get the relevant ServerPlayerEntity from the passed PlayerEntity
        if(player instanceof ServerPlayerEntity serverPlayer) {
            if(ownedBy(player)) {
                //Send a packet to the client to open the camp supplies GUI
                ServerPlayNetworking.send(serverPlayer, new ShowGUIPayload(false));
            } else if(uuid == null) {
                //TODO re-decide if the player should be able to claim supplies via a pop-up menu
                setOwner(player);
                ServerPlayNetworking.send(serverPlayer, new ShowGUIPayload(false));
            } else {
                serverPlayer.sendMessage(Text.of("These supplies are owned by " + getOwnerName()), true);
            }
        }
    }

    public boolean setOwner(PlayerEntity player) {
        if(uuid == null) {
            uuid = player.getUuid();
            ownerName = player.getNameForScoreboard();
            return true;
        }
        return false;
    }

    public boolean ownedBy(PlayerEntity player) {
        if(uuid == null) return false;
        return uuid.equals(player.getUuid());
    }

    public String getOwnerName() {
        try {
            return ownerName;
        } catch(NullPointerException e) {
            return "Mr. Error.";
            //return "an offline player";
        }
    }

    // endregion OWNERSHIP

    // region STRUCTURES
    public static boolean placeStructure(ServerPlayerEntity caller, StructureSlot slot, BlockPos origin) {
        if(slot.isPlaced())
        {
            System.out.println(slot.structureName + " is already placed!");
            return false;
        }

        ServerWorld world = caller.getServerWorld();

        // Check if proposed placement overlaps anything
        // ppa is short for proposedPlacementArea
        BlockBox ppa = slot.getProposedArea(origin);
        for(BlockPos pos : BlockPos.iterate(
                ppa.getMinX(), ppa.getMinY(), ppa.getMinZ(),
                ppa.getMaxX(), ppa.getMaxY(), ppa.getMaxZ())) {
            if(!world.getBlockState(pos).isAir()) {
                System.out.println("The proposed area has blocks in it!");
                return false;
            }
        }

        // Get a StructureTemplate, given the name of a structure
        StructureTemplate template;
        StructureTemplateManager templateManager = world.getStructureTemplateManager();
        try {
            template = templateManager.getTemplateOrBlank(slot.structureFileName);
        } catch (InvalidIdentifierException e) {
            return false;
        }

        // place the template
        StructurePlacementData structurePlacementData = (new StructurePlacementData())/*.setMirror(this.mirror).setRotation(this.rotation).setIgnoreEntities(this.ignoreEntities)*/;

        boolean result = template.place(
                world,
                origin,
                new BlockPos(0, 0, 0),
                structurePlacementData,
                null,
                2);

        if (result)
        {
            slot.place(origin);
            System.out.println("Successfully placed " + slot.structureName);
            // TODO this might be overkill but we need to update the clientside slots when changes are made
            // This version isn't as much overkill as calling updateStructureSlots every time, but is still kind of a lot.
            returnUpdatedSlot(caller, slot);

            return true;
        }

        System.out.println("Failed to place " + slot.structureName);
        return false;
    }

    public static boolean removeStructure(ServerPlayerEntity caller, StructureSlot slot) {
        if (!slot.isPlaced()) {
            System.out.println(slot.structureName + " is not yet placed!");
            return false;
        }

        if (saveStructure(caller.getServerWorld(), slot, new BlockPos(
                slot.getOccupiedArea().getMinX(),
                slot.getOccupiedArea().getMinY(),
                slot.getOccupiedArea().getMinZ()
        ), caller.getUuidAsString())) {
            //fillArea(caller.getServerWorld(), slot.getOccupiedArea(), Blocks.AIR.getDefaultState());
            fancyFillArea(caller.getServerWorld(), slot.getOccupiedArea(), Blocks.AIR.getDefaultState());

            slot.remove();
            // TODO this might be overkill but we need to update the clientside slots when changes are made
            // This version isn't as much overkill as calling updateStructureSlots every time, but is still kind of a lot.
            returnUpdatedSlot(caller, slot);
        }

        return false;
    }

    public static boolean saveStructure(ServerWorld world, StructureSlot slot, BlockPos origin, String authorUuid) {
        //convert structureSize to a Vec3i
        Vec3i structureSizeInt = new Vec3i(
                slot.sizeX(),
                slot.sizeY(),
                slot.sizeZ()
        );

        StructureTemplateManager templateManager = world.getStructureTemplateManager();
        StructureTemplate structureTemplate;
        try {
            structureTemplate = templateManager.getTemplateOrBlank(slot.structureFileName);
        } catch (InvalidIdentifierException e) {
            return false;
        }
        //Write the structure from the world to the template
        structureTemplate.saveFromWorld(world, origin, structureSizeInt, true, Blocks.BEDROCK);
        structureTemplate.setAuthor(authorUuid);
        //Save the template
        try {
            System.out.println("Successfully saved " + slot.structureFileName);
            return templateManager.saveTemplate(slot.structureFileName);
        } catch (InvalidIdentifierException e) {
            return false;
        }

    }

    private static boolean fillArea(ServerWorld world, BlockBox area, BlockState state) {
        // Fill the area
        for (BlockPos pos : BlockPos.iterate(
                new BlockPos(area.getMinX(), area.getMinY(), area.getMinZ()),
                new BlockPos(area.getMaxX(), area.getMaxY(), area.getMaxZ()))) {
            // TODO try and compare against a list of blocks using BlockState.isIn(TagKey<Block>)
            //  world.getBlockState(new BlockPos(x, y, z)).isIn(\* something *\);
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
        }
        // Mark changed chunks dirty?

        return true;
    }

    // TODO I really love the look of this effect, but it might be exploitable if players mine
    //  and collect blocks before they're removed. It's probably better to replace all the actual
    //  blocks with display entities or something beforehand.
    private static boolean fancyFillArea(ServerWorld world, BlockBox area, BlockState state) {
        Thread fancyFillThread = new Thread(() -> _fancyFillArea(world, area, state));
        fancyFillThread.start();

        return true;
    }

    private static final int TICK_DELAY_BETWEEN_REMOVALS = 10;

    private static void _fancyFillArea(ServerWorld world, BlockBox area, BlockState state) {
        int layerSize = area.getBlockCountX() * area.getBlockCountZ();
        int layerCounter = 0;

        // Fill the area
        try {
            for (BlockPos pos : BlockPos.iterate(
                    new BlockPos(area.getMinX(), area.getMinY(), area.getMinZ()),
                    new BlockPos(area.getMaxX(), area.getMaxY(), area.getMaxZ()))) {
                // TODO try and compare against a list of blocks using BlockState.isIn(TagKey<Block>)
                //  world.getBlockState(new BlockPos(x, y, z)).isIn(\* something *\);
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
                layerCounter++;

                if (layerCounter >= layerSize) {
                    // TODO find a more elegant way to wait a little bit before continuing.
                    //  Try to sync it to game ticks?
                    layerCounter = 0;
                    Thread.sleep(50);
                }
            }
        } catch (InterruptedException e) {
            // Fall back to the boring method if something goes south.
            System.out.println("Interrupted :(");
            fillArea(world, area, state);
        }

        // Mark changed chunks dirty?
    }

    // endregion STRUCTURES

    // region NETWORKING
    private static void updateStructureSlots(ServerPlayerEntity player) {
        Path structureDirectory = player.server
                .getSavePath(WorldSavePath.GENERATED)
                .resolve(NomadsCamps.MOD_ID)
                //TODO reimplement each player having their own structure directory
                //.resolve(player.getNameForScoreboard().toLowerCase())
                .resolve("structures");

        ServerPlayNetworking.send(player, new ReturnSlotsPayload(NomadsCamps.getStructureSlotsFromFile(structureDirectory)));
    }

    private static void returnUpdatedSlot(ServerPlayerEntity player, StructureSlot slot) {
        ArrayList<StructureSlot> list = new ArrayList<>();
        list.add(slot);

        ServerPlayNetworking.send(player, new ReturnSlotsPayload(list));
    }
    // endregion NETWORKING

    // region DATA SAVING
    public UUID getUuid() { return uuid; }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        //BlockEntity is an abstract class, so this shouldn't be necessary
        //but if something breaks, uncomment it.
        super.writeNbt(nbt, registries);
        if(uuid != null)
            nbt.putUuid("uuid", uuid);
        if(ownerName != null)
            nbt.putString("owner", ownerName);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        //super.readNbt(nbt, registries);
        if(nbt.contains("uuid")) uuid = nbt.getUuid("uuid");
        if(nbt.contains("owner")) ownerName = nbt.getString("owner");
    }
    // endregion DATA SAVING
}
