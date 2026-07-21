package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.ShowGUIPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

public class CampBlockEntity extends BlockEntity {

    // region FIELDS
    private BlockPos pos;

    // TODO have these pull from the .config
    private int numStructureSlots = 4;
    private ArrayList<StructureSlot> structureSlots;
    // endregion FIELDS

    //Constructor
    public CampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMP_BLOCK_ENTITY, pos, state);
        this.pos = pos;
        structureSlots = new ArrayList<>(numStructureSlots);
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
            populateStructureSlots();
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

    // region STRUCTURE SLOTS
    private void populateStructureSlots() {
        // Look in the files for a player's list of structure slots
        // While this block is placed, it will have the structures stored in it for easy access.
        assert this.getWorld() != null;
        assert this.getWorld().getServer() != null;
        Path directory = this.getWorld().getServer()
                .getSavePath(WorldSavePath.GENERATED)
                .resolve(NomadsCamps.MOD_ID)
                //.resolve(ownerName.toLowerCase())
                .resolve("structures");

        structureSlots = NomadsCamps.getStructureSlotsFromFile(directory);
    }

    private void writeStructureSlots() {
        assert this.getWorld() != null;
        assert this.getWorld().getServer() != null;
        Path directory = this.getWorld().getServer()
                .getSavePath(WorldSavePath.GENERATED)
                .resolve(NomadsCamps.MOD_ID)
                //.resolve(ownerName.toLowerCase())
                .resolve("structures");

        NomadsCamps.writeStructureSlotsToFile(directory, this.getStructureSlots());
    }
    // endregion STRUCTURE SLOTS

    // region STRUCTURES
    public static boolean placeStructure(ServerWorld world, StructureSlot slot, BlockPos origin) {
        if(slot.isPlaced())
        {
            System.out.println(slot.structureName + " is already placed!");
            return false;
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

            return true;
        }

        System.out.println("Failed to place " + slot.structureName);
        return false;
    }

    public static boolean removeStructure(ServerWorld world, StructureSlot slot, String author) {
        if (!slot.isPlaced()) {
            System.out.println(slot.structureName + " is not yet placed!");
            return false;
        }

        if (saveStructure(world, slot, new BlockPos(
                slot.getOccupiedArea().getMinX(),
                slot.getOccupiedArea().getMinY(),
                slot.getOccupiedArea().getMinZ()
        ), author)) {
            // TODO fill slot.occupiedArea with air.
        }

        return false;
    }

    public static boolean saveStructure(ServerWorld world, StructureSlot slot, BlockPos origin, String author) {
        Identifier structureName = Identifier.of(slot.structureName);

        //convert structureSize to a Vec3i
        Vec3i structureSizeInt = new Vec3i(
                slot.sizeX(),
                slot.sizeY(),
                slot.sizeZ()
        );

        StructureTemplateManager templateManager = world.getStructureTemplateManager();
        StructureTemplate structureTemplate;
        try {
            structureTemplate = templateManager.getTemplateOrBlank(structureName);
        } catch (InvalidIdentifierException e) {
            return false;
        }
        //Write the structure from the world to the template
        structureTemplate.saveFromWorld(world, origin, structureSizeInt, true, Blocks.BEDROCK);
        structureTemplate.setAuthor(author);
        //Save the template
        try {
            System.out.println("Successfully saved " + structureName);
            return templateManager.saveTemplate(structureName);
        } catch (InvalidIdentifierException e) {
            return false;
        }

    }

    // endregion STRUCTURES

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

        //TODO this might cause performance issues since we don't need to write it every time
        //TODO also the writeNbt method might not be called if the supplies are placed when the world is closed
        //writeStructureSlots();
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        //super.readNbt(nbt, registries);
        if(nbt.contains("uuid")) uuid = nbt.getUuid("uuid");
        if(nbt.contains("owner")) ownerName = nbt.getString("owner");
        populateStructureSlots();
    }
    // endregion DATA SAVING

    // region GETTERS
    public int getNumStructureSlots() { return numStructureSlots; }
    public ArrayList <StructureSlot> getStructureSlots() { return structureSlots; }
    // endregion GETTERS
}
