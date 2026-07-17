package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NomadsCamps implements ModInitializer {
    // TODO fill out fabric.mod.json and README.md
    public static final String MOD_ID = "nomads-camps";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    // TODO replace sysout printlns with logger writes
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        ModBlocks.initialize();
        ModBlockEntities.initialize();

        // region NETWORKING
        PayloadTypeRegistry.playS2C().register(CampSuppliesGUIPayload.ID, CampSuppliesGUIPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CampBlockSetOwnerPayload.ID, CampBlockSetOwnerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CampBlockSavePayload.ID, CampBlockSavePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CampBlockBuildPayload.ID, CampBlockBuildPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(QueryStructuresPayload.ID, QueryStructuresPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ReturnStructuresPayload.ID, ReturnStructuresPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ReturnStructureSlotsPayload.ID, ReturnStructureSlotsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CampBlockRemovePayload.ID, CampBlockRemovePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CampBlockSetOwnerPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();
                    CampBlockEntity supplies = getCampBlockAtPos(payload.suppliesPos(), sender);

                    if(supplies.setOwner(sender))
                        System.out.println("Owner set!");
                });

        ServerPlayNetworking.registerGlobalReceiver(CampBlockSavePayload.ID,
                (payload, context) -> {
                    //access payload data using payload.fieldName();
                    ServerPlayerEntity sender = context.player();
                    CampBlockEntity supplies = getCampBlockAtPos(payload.suppliesPos(), sender);

                    if(!supplies.saveStructure(payload.structName(), payload.origin(), payload.size()))
                        System.out.println("Structure failed to save!");
                });

        ServerPlayNetworking.registerGlobalReceiver(CampBlockBuildPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();
                    CampBlockEntity supplies = getCampBlockAtPos(payload.suppliesPos(), sender);

                    if(!supplies.placeStructure((ServerWorld) sender.getWorld(), payload.structureName(), payload.origin()))
                            System.out.println("Structure failed to place!");
                });

        // Old implementation; only returns the names of structures when queried
//        ServerPlayNetworking.registerGlobalReceiver(QueryStructuresPayload.ID,
//                (payload, context) -> {
//                    ServerPlayerEntity sender = context.player();
//
//                    Path structureDirectory = sender.server
//                            .getSavePath(WorldSavePath.GENERATED)
//                            .resolve(MOD_ID)
//                            //TODO reimplement each player having their own structure directory
//                            //.resolve(sender.getNameForScoreboard().toLowerCase())
//                            .resolve("structures");
//
//                    // Return to sender!
//                    ServerPlayNetworking.send(sender, new ReturnStructuresPayload(getKnownStructuresFromFile(structureDirectory)));
//                });

        // New implementation; returns the list of StructureSlots when queried
        ServerPlayNetworking.registerGlobalReceiver(QueryStructuresPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();
                    ArrayList<StructureSlot> slots = getCampBlockAtPos(payload.suppliesPos(), sender).getStructureSlots();;

                    // Return to sender!
                    ServerPlayNetworking.send(sender, new ReturnStructureSlotsPayload(slots));
                });
        // endregion NETWORKING
	}

    // region HELPER METHODS
    public ArrayList<String> getKnownStructuresFromFile(Path structureDirectory) {
        try(Stream<Path> files = Files.list(structureDirectory)) {
            List<Path> list = files.filter(Files::isRegularFile).toList();
            ArrayList<String> structureNames = new ArrayList<>();
            for(Path file : list) {
                structureNames.add(file.subpath(file.getNameCount() - 1, file.getNameCount()).toString());
            }
            return structureNames;
        } catch(IOException e) {
            //IDK man
            return null;
        }
    }

    public static ArrayList<StructureSlot> getStructureSlotsFromFile(Path structureDirectory) {
        Path file = structureDirectory.resolve("slots.json");
        ArrayList<StructureSlot> structureSlots = new ArrayList<>();

        Gson jsonParser = new GsonBuilder().create();
        if(Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                structureSlots = (jsonParser.fromJson(reader, new TypeToken<ArrayList<StructureSlot>>(){}));
            } catch (IOException e) {
                //IDK man
                return null;
            }
        }

        return structureSlots;
    }

    public static boolean writeStructureSlotsToFile(Path structureDirectory, CampBlockEntity supplies) {
        Path target = structureDirectory.resolve("slots.json");
        Gson jsonParser = new GsonBuilder().create();

        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            return false;
        }
        try(BufferedWriter writer = Files.newBufferedWriter(target)) {
            jsonParser.toJson(supplies.getStructureSlots(), writer);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private CampBlockEntity getCampBlockAtPos(BlockPos pos, ServerPlayerEntity sender)
    {
        BlockEntity maybeSupplies = sender.getWorld().getBlockEntity(pos);

        if(maybeSupplies instanceof CampBlockEntity supplies) {
            return supplies;
        } else {
            //TODO add dimension/world checking. Might already be handled by sender.getWorld()
            throw(new NullPointerException("Given location does not contain camp supplies!"));
        }
    }
    // endregion HELPER METHODS
}