/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.*;
import com.ghosty.nomadscamps.util.NomadsCampsConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// TODO fill out README.md

/// Controls general logic for the mod. Handles networking and reading/writing files.
public class NomadsCamps implements ModInitializer {
    /// The mod id. Kept in a central location in case it needs to be changed.
    public static final String MOD_ID = "nomads-camps";
    /// The filename used to indicate a structure slot that has never been placed.
    /// Cannot be used as a structure filename.
    public static final Identifier DEFAULT_STRUCTURE_FILENAME = Identifier.of(MOD_ID + ":null");
    /// A reference to the config wrapper class.
    ///
    /// @see com.ghosty.nomadscamps.util.NomadsCampsConfigModel
    public static final NomadsCampsConfig CONFIG = NomadsCampsConfig.createAndLoad();
    /// Logger for writing to the console and log file.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /// Called by the game once it is ready to load mods. Initializes the block and
    /// block entity registries. Registers all networking payloads and defines the
    /// client-bound payload handlers.
    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModBlockEntities.initialize();

        // region NETWORKING
        // Server-bound payloads
        PayloadTypeRegistry.playC2S().register(StructureActionPayload.ID, StructureActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateSlotsPayload.ID, UpdateSlotsPayload.CODEC);

        // Client-bound payloads
        PayloadTypeRegistry.playS2C().register(ShowGUIPayload.ID, ShowGUIPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ReturnSlotsPayload.ID, ReturnSlotsPayload.CODEC);

        // Handler for the Structure Action payload. Places or removes the
        // given structure, depending on the payload type.
        ServerPlayNetworking.registerGlobalReceiver(StructureActionPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();

                    switch (payload.type()) {
                        //Case 1 is "build"
                        case 1:
                            CampBlockEntity.placeStructure(sender, payload.slot(), payload.origin());
                            break;
                        //Case 2 is "remove"
                        case 2:
                            CampBlockEntity.removeStructure(sender, payload.slot());
                            break;
                        default:
                            break;
                    }
                });

        // Handler for the Update Slots payload. Writes the received slots
        // to file if they've been changed. Builds a list of structure
        // slots from file and sends it back if no slots have been changed.
        ServerPlayNetworking.registerGlobalReceiver(UpdateSlotsPayload.ID,
                (payload, context) -> {
                    Path structureDirectory = context.player().server
                            .getSavePath(WorldSavePath.GENERATED)
                            .resolve(MOD_ID)
                            .resolve(context.player().getNameForScoreboard().toLowerCase());

                    if (payload.changed()) {
                        // Write the received slot list into the files
                        writeStructureSlotsToFile(structureDirectory, payload.slots());
                    } else {
                        // Build the full slot list from file and send it back.
                        ServerPlayNetworking.send(
                                context.player(),
                                new ReturnSlotsPayload(
                                        getStructureSlotsFromFile(structureDirectory)
                                ));
                    }
                });
        // endregion NETWORKING
    }

    // region HELPER METHODS

    /// Builds an ArrayList of structure slots from the slots.json file at the given directory.
    ///
    /// @param structureDirectory The location of the slots.json file to build from.
    /// @return The constructed ArrayList of slots.
    public static ArrayList<StructureSlot> getStructureSlotsFromFile(Path structureDirectory) {
        Path file = structureDirectory.resolve("slots.json");
        ArrayList<StructureSlot> structureSlots;
        Gson jsonParser = new GsonBuilder().create();

        if (Files.exists(file)) {
            // If a slots.json file is present at the given location, read it.
            try (Reader reader = Files.newBufferedReader(file)) {
                structureSlots = (jsonParser.fromJson(reader, new TypeToken<>() {
                }));
            } catch (IOException e) {
                LOGGER.error("An error occurred while reading a player's structure slot data!", e);
                return new ArrayList<>();
            }
        } else {
            // If no file was present, build one using the default values.
            structureSlots = getDefaultStructureSlots();
            writeStructureSlotsToFile(structureDirectory, structureSlots);
        }

        return structureSlots;
    }

    /// Saves the passed list of structure slots to file.
    ///
    /// @param structureDirectory The location of the slots.json file to write into or the
    ///                           location where it should be created.
    /// @param slots              The list of slots to write to file.
    /// @return True if the file was saved successfully; false otherwise.
    public static boolean writeStructureSlotsToFile(Path structureDirectory, ArrayList<StructureSlot> slots) {
        Path target = structureDirectory.resolve("slots.json");
        Gson jsonParser = new GsonBuilder().create();

        // Create the slots.json file if it doesn't exist
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating a slot data file!", e);
            return false;
        }
        // Write the passed list into the file
        try (BufferedWriter writer = Files.newBufferedWriter(target)) {
            jsonParser.toJson(slots, writer);
        } catch (IOException e) {
            LOGGER.error("An error occurred while writing data to a slot data file!", e);
            return false;
        }

        return true;
    }

    // TODO Make the multithreading here safer. If a new structure is registered
    //  while another player's thread is finding a file name, a duplicate may be made.

    /// Returns a list of all registered structure file names.
    ///
    /// @param structureDirectory The directory to look in for structure files.
    /// @return A list containing a path for each structure file.
    public static List<Path> getStructureFileNames(Path structureDirectory) {
        try (Stream<Path> files = Files.list(structureDirectory)) {
            return files.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            LOGGER.error("An error occurred while reading structure filenames!", e);
            return new ArrayList<>();
        }
    }

    /// Builds a list of structure slots using the default values found in the .config file.
    ///
    /// @return An ArrayList of structure slots made using default values.
    private static ArrayList<StructureSlot> getDefaultStructureSlots() {
        ArrayList<StructureSlot> output = new ArrayList<>(CONFIG.startingSlotCount());

        for (int i = 0; i < CONFIG.startingSlotCount(); i++)
            output.add(new StructureSlot(i));

        return output;
    }
    // endregion HELPER METHODS
}