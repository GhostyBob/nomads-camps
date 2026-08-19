/// @Author GhostyBob
/// @Version 8/18/26

package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.util.TaggedStructureTemplate;
import com.ghosty.nomadscamps.networking.ShowGUIPayload;
import com.ghosty.nomadscamps.util.ModTags;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
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
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.nio.file.Path;
import java.util.*;

/// Defines behavior for the Camp Supplies block entity.
public class CampBlockEntity extends BlockEntity {
    /// The uuid for this block entity. Null if these supplies have no
    /// owner. Set to the owner's player uuid if these supplies are owned.
    private UUID uuid = null;
    /// A human-readable representation of these supplies' owner. Null
    /// if there is no owner.
    private String ownerName = null;
    /// The number of upgrades that have been installed in these camp
    /// supplies since the last time it was interacted with.
    private int unsavedUpgrades = 0;

    /// Trivial constructor; constructs a CampBlockEntity with the
    /// given state at the given position using the default
    /// BlockEntity constructor.
    public CampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMP_BLOCK_ENTITY, pos, state);
    }

    /// Displays the camp supplies GUI to the passed player if these
    /// supplies are unowned or the passed player is the owner. Will
    /// just display a message listing the owner name if the passed
    /// player is not the owner.
    ///
    /// @param player The player to possibly show the GUI to.
    public void showGUI(PlayerEntity player) {
        // Get the relevant ServerPlayerEntity from the passed PlayerEntity.
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (ownedBy(player)) {
                // Save the unsaved upgrades before showing the GUI
                UpgradeTracker upgrades = NomadsCamps.adjustSlotUpgrades(
                        serverPlayer.server,
                        serverPlayer.getNameForScoreboard().toLowerCase(),
                        unsavedUpgrades
                );
                unsavedUpgrades = 0;

                // Send a packet to the client to open the camp supplies GUI.
                ServerPlayNetworking.send(serverPlayer, new ShowGUIPayload(
                        pos,
                        upgrades));
                return;
            }
            // Set these supplies' owner to the passed player, then show
            // them the GUI.
            if (uuid == null && setOwner(player)) {
                // Save the unsaved upgrades before showing the GUI
                UpgradeTracker upgrades = NomadsCamps.adjustSlotUpgrades(
                        serverPlayer.server,
                        serverPlayer.getNameForScoreboard().toLowerCase(),
                        unsavedUpgrades
                );
                unsavedUpgrades = 0;

                ServerPlayNetworking.send(serverPlayer, new ShowGUIPayload(
                        pos,
                        upgrades));
                return;
            }

            // Display a message to the passed player that these supplies are
            // owned by someone else.
            serverPlayer.sendMessage(Text.of("These supplies are owned by " + getOwnerName()), true);
        }
    }

    /// If these supplies are unowned, set their owner as the passed player.
    ///
    /// @param player The player to try and set as the new owner.
    /// @return True if the owner was updated to player; false otherwise.
    public boolean setOwner(PlayerEntity player) {
        if (uuid == null) {
            uuid = player.getUuid();
            ownerName = player.getNameForScoreboard();
            return true;
        }
        return false;
    }

    /// Checks if these supplies are owned by the passed player. Compares the
    /// uuid stored in these supplies to the passed player's uuid using uuid.equals().
    ///
    /// @param player The player being compared to these supplies' owner.
    /// @return False if these supplies have no owner or the owner's uuid
    /// doesn't equal that of the passed player.
    public boolean ownedBy(PlayerEntity player) {
        if (uuid == null) return false;
        return uuid.equals(player.getUuid());
    }

    /// @return The contents of the ownerName field, or the string "null" if
    /// it is empty.
    public String getOwnerName() {
        try {
            return ownerName;
        } catch (NullPointerException e) {
            return "null";
        }
    }

    /// Handles the placement of structure slots using the same general process as
    /// the base game's structure blocks.
    ///
    /// @param caller The ServerPlayerEntity that requested this structure be placed.
    ///                             Used to place the structure in the correct world, find the right directory
    ///                             for structure files, and is the recipient of a message if the structure
    ///                             can't be placed.
    /// @param slot   The StructureSlot to pull the structure filename and other data from.
    ///                             Mutated by this method and sent back to caller to be saved.
    /// @param origin The position to anchor the placed structure to. Anchors the
    ///                             structure using the (-x, -y, -z) corner.
    /// @return True if the structure was placed successfully; false otherwise.
    /// @see net.minecraft.block.entity.StructureBlockBlockEntity
    public static boolean placeStructure(ServerPlayerEntity caller, StructureSlot slot, BlockPos origin) {
        boolean result;

        if (!slot.structureFileName.equals(NomadsCamps.DEFAULT_STRUCTURE_FILENAME)) {
            // If the slot's filename isn't the default, find and place the existing structure.
            if (slot.isPlaced()) {
                NomadsCamps.LOGGER.debug("Tried to place {}, but it was already placed!", slot.structureName);
                return false;
            }

            ServerWorld world = caller.getServerWorld();

            // Check if the proposed placement overlaps anything
            // ppa is short for proposedPlacementArea
            BlockBox ppa = slot.getProposedArea(origin);
            for (BlockPos pos : BlockPos.iterate(
                    ppa.getMinX(), ppa.getMinY(), ppa.getMinZ(),
                    ppa.getMaxX(), ppa.getMaxY(), ppa.getMaxZ())) {
                // TODO Make this check more permissive by checking against a TagKey.
                if (!world.getBlockState(pos).isAir()) {
                    caller.sendMessage(Text.of("The proposed area is obstructed! No structure was placed."), true);
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

            // Place the template
            StructurePlacementData structurePlacementData = (new StructurePlacementData());

            result = template.place(
                    world,
                    origin,
                    new BlockPos(0, 0, 0),
                    structurePlacementData,
                    null,
                    2);
        } else {
            // Logic for placing a new slot for the first time

            // region FILENAME FINDING
            // Get a valid filename
            String name = slot.structureName.toLowerCase(Locale.ROOT);
            Identifier proposedFilename = Identifier.tryParse(NomadsCamps.MOD_ID, name);
            if (proposedFilename == null) {
                // Logic for manually building a valid file name
                StringBuilder builder = new StringBuilder();
                int index, prevIndex = 0;
                for (index = 0; index < name.length(); index++)
                    if (!Identifier.isCharValid(name.charAt(index))) {
                        if (index - prevIndex > 0)
                            builder.append(name, prevIndex, index);
                        prevIndex = index + 1;
                    }
                proposedFilename = Identifier.of(NomadsCamps.MOD_ID, builder.toString());
            }

            // Don't allow actual structure files to have the default filename
            if (proposedFilename.equals(NomadsCamps.DEFAULT_STRUCTURE_FILENAME))
                proposedFilename = Identifier.of(adjustFilename(proposedFilename));

            // Check other filenames for duplicates
            List<Path> filenames = NomadsCamps.getStructureFileNames(caller.server
                    .getSavePath(WorldSavePath.GENERATED)
                    .resolve(NomadsCamps.MOD_ID)
                    .resolve("structures"));

            boolean checkingDuplicates;
            do {
                checkingDuplicates = false;
                for (Path p : filenames) {
                    // Shorten p to only include the file name itself.
                    String pName = p.subpath(p.getNameCount() - 1, p.getNameCount()).toString();
                    // Cut off the file extension, then compare it to the proposed filename.
                    if (pName.substring(0, (pName.length() - 4)).equals(proposedFilename.getPath())) {
                        // Filename is a duplicate: Adjust the name and start over
                        checkingDuplicates = true;
                        proposedFilename = Identifier.of(NomadsCamps.MOD_ID, adjustFilename(proposedFilename));
                        break;
                    }
                }
            } while (checkingDuplicates);
            NomadsCamps.LOGGER.debug("Created a new structure file with identifier {}", proposedFilename);
            // endregion FILENAME FINDING

            // At this point, proposedFilename is a valid Identifier and isn't a duplicate.
            // Make a new template with the proposed file name.
            slot.structureFileName = proposedFilename;
            StructureTemplateManager templateManager = caller.getServerWorld().getStructureTemplateManager();
            templateManager.getTemplateOrBlank(proposedFilename);
            result = templateManager.saveTemplate(proposedFilename);
        }

        // If no problems have happened yet, mark the slot as placed and send it
        // to the client to be updated.
        if (result) {
            slot.place(origin);
            NomadsCamps.LOGGER.debug("Successfully placed {}.", slot.structureName);
            NomadsCamps.returnUpdatedSlot(caller, slot);

            return true;
        }

        NomadsCamps.LOGGER.debug("Failed to place {}.", slot.structureName);
        return false;
    }

    /// Helper method for adjusting structure filenames to avoid duplicates. Appends a number
    /// to the end of the file name, or increments it if there is already a number.
    ///
    /// @param workingName The filename that needs to be adjusted.
    /// @return A string representation of the adjusted filename.
    private static String adjustFilename(Identifier workingName) {
        StringBuilder newNameBuilder = new StringBuilder(workingName.getPath());
        StringBuilder newNameSuffixBuilder = new StringBuilder();

        // Starting at the end, move toward the start until a non-number char is encountered.
        for (int i = newNameBuilder.length() - 1;
             (newNameBuilder.charAt(i) >= '0' && newNameBuilder.charAt(i) <= '9');
             i--) {
            newNameSuffixBuilder.insert(0, newNameBuilder.charAt(i));
        }


        int newNameSuffix = 0;
        if (!newNameSuffixBuilder.isEmpty()) {
            // Remove the characters stored in newNameSuffixBuilder from newNameBuilder
            newNameBuilder.delete(newNameBuilder.length() - newNameSuffixBuilder.length(), newNameBuilder.length());

            // Increment the number contained in newNameSuffixBuilder, or use 0 if it is empty.
            newNameSuffix = Integer.parseInt(newNameSuffixBuilder.toString());
            newNameSuffix++;
        }

        newNameBuilder.append(newNameSuffix);
        return newNameBuilder.toString();
    }

    /// Handles the removal of structures from the world and saves them to file.
    ///
    /// @param caller The ServerPlayerEntity that requested the structure be removed.
    ///                             Used to remove from the correct world and receives the updated slot.
    /// @param slot   The structure slot to pull data from. Mutated by this method and
    ///                             sent back to caller to be saved.
    /// @return True if the structure was removed successfully; false otherwise.
    public static boolean removeStructure(ServerPlayerEntity caller, StructureSlot slot) {
        if (!slot.isPlaced()) {
            NomadsCamps.LOGGER.debug("Tried to remove {}, but it is not yet placed!", slot.structureName);
            return false;
        }

        assert slot.getOccupiedArea() != null;
        if (saveStructure(caller.getServerWorld(), slot, new BlockPos(
                slot.getOccupiedArea().getMinX(),
                slot.getOccupiedArea().getMinY(),
                slot.getOccupiedArea().getMinZ()
        ), caller.getUuidAsString())) {
            fillArea(caller.getServerWorld(), slot.getOccupiedArea(), Blocks.AIR.getDefaultState());
            //fancyFillArea(caller.getServerWorld(), slot.getOccupiedArea(), Blocks.AIR.getDefaultState());

            slot.remove();
            NomadsCamps.returnUpdatedSlot(caller, slot);
            return true;
        }

        return false;
    }

    /// Handles the saving of structures in a similar fashion as the base game's structure blocks.
    ///
    /// @param world      The world to save the structure from.
    /// @param slot       The structure slot to pull data from. Not mutated by this method.
    /// @param origin     The blockPos to anchor to while saving. Anchors to the (-x, -y, -z) corner.
    /// @param authorUuid The uuid of the structure's author. Usually the uuid of the player
    ///                                     that requested this structure be saved.
    /// @return True if the structure was saved successfully; false otherwise.
    /// @see net.minecraft.block.entity.StructureBlockBlockEntity
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
        ((TaggedStructureTemplate) structureTemplate).nomads_camps$taggedSaveFromWorld(world, origin, structureSizeInt, true, ModTags.Blocks.PACKING_IGNORED_BLOCKS);
        structureTemplate.setAuthor(authorUuid);
        //Save the template
        try {
            NomadsCamps.LOGGER.debug("Successfully saved {}.", slot.structureFileName);
            return templateManager.saveTemplate(slot.structureFileName);
        } catch (InvalidIdentifierException e) {
            return false;
        }

    }

    /// A quick and dirty way to fill an area with one BlockState. Works in a
    /// similar way to the base game's /fill command. Doesn't replace blocks
    /// in the PACKING_IGNORED_BLOCKS tag.
    ///
    /// @param world The world to replace blocks in.
    /// @param area  All applicable blocks in this area will be set to state.
    /// @param state The BlockState to set everything to.
    /// @see ModTags
    private static void fillArea(ServerWorld world, BlockBox area, BlockState state) {
        for (BlockPos pos : BlockPos.iterate(
                new BlockPos(area.getMinX(), area.getMinY(), area.getMinZ()),
                new BlockPos(area.getMaxX(), area.getMaxY(), area.getMaxZ()))) {
            if (!world.getBlockState(pos).isIn(ModTags.Blocks.PACKING_IGNORED_BLOCKS))
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
        }
    }

    // TODO Refine this method of filling so it's less exploitable.
    // region FANCY FILL AREA

    /// Driver method for a nicer-looking way to fill an area with one BlockState. Works
    /// in a similar way to the base game's /fill command, but fills in slices with a
    /// slight delay between each slice. Doesn't replace blocks in the
    /// PACKING_IGNORED_BLOCKS tag. This method just starts up a thread handling the
    /// actual removal.
    ///
    /// @param world The world to replace blocks in.
    /// @param area  All applicable blocks in this area will be set to state.
    /// @param state The BlockState to set everything to.
    private static void fancyFillArea(ServerWorld world, BlockBox area, BlockState state) {
        Thread fancyFillThread = new Thread(() -> _fancyFillArea(world, area, state));
        fancyFillThread.start();
    }

    /// A nicer-looking way to fill an area with one BlockState. Works in a similar way
    /// to the base game's /fill command, but fills in slices with a slight delay between
    /// each slice. Doesn't replace blocks in the PACKING_IGNORED_BLOCKS tag.
    ///
    /// @param world The world to replace blocks in.
    /// @param area  All applicable blocks in this area will be set to state.
    /// @param state The BlockState to set everything to.
    private static void _fancyFillArea(ServerWorld world, BlockBox area, BlockState state) {
        int layerSize = area.getBlockCountX() * area.getBlockCountZ();
        int layerCounter = 0;

        // Fill the area
        try {
            for (BlockPos pos : BlockPos.iterate(
                    new BlockPos(area.getMinX(), area.getMinY(), area.getMinZ()),
                    new BlockPos(area.getMaxX(), area.getMaxY(), area.getMaxZ()))) {
                if (!world.getBlockState(pos).isIn(ModTags.Blocks.PACKING_IGNORED_BLOCKS))
                    world.setBlockState(pos, state, Block.NOTIFY_ALL);
                layerCounter++;

                if (layerCounter >= layerSize) {
                    layerCounter = 0;
                    Thread.sleep(50);
                }
            }
        } catch (InterruptedException e) {
            // Fall back to the simple method if something goes south.
            NomadsCamps.LOGGER.debug("The structure removal process was interrupted.");
            fillArea(world, area, state);
        }
    }
    // endregion FANCY FILL AREA

    /// Used by the base game to put an entity's data into an NBT compound.
    ///
    /// @param nbt The nbt compound being written into. Mutated by this
    ///            method and should be treated as its output.
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (uuid != null)
            nbt.putUuid("uuid", uuid);
        if (ownerName != null)
            nbt.putString("owner", ownerName);
        nbt.putInt("upgrades", unsavedUpgrades);
    }

    /// Used by the base game to set up an entity's data, given an NBT compound.
    ///
    /// @param nbt The nbt compound being read from. Not mutated by this method.
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        if (nbt.contains("uuid")) uuid = nbt.getUuid("uuid");
        if (nbt.contains("owner")) ownerName = nbt.getString("owner");
        if (nbt.contains("upgrades")) unsavedUpgrades = nbt.getInt("upgrades");
    }
}
