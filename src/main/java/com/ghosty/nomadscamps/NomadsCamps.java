package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.*;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class NomadsCamps implements ModInitializer {
    public static final String MOD_ID = "nomads-camps";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    // TODO replace sysout printlns with logger writes
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Path structureDirectory;

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

        ServerPlayNetworking.registerGlobalReceiver(CampBlockSetOwnerPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();
                    BlockEntity maybeSupplies = sender.getWorld().getBlockEntity(payload.suppliesPos());

                    if(maybeSupplies instanceof CampBlockEntity supplies) {
                        if(supplies.setOwner(sender))
                            System.out.println("Owner set!");
                    } else {
                        throw(new NullPointerException("Given location does not contain camp supplies! [OWN]"));
                    }
                });

        ServerPlayNetworking.registerGlobalReceiver(CampBlockSavePayload.ID,
                (payload, context) -> {
                    //access payload data using payload.fieldName();
                    ServerPlayerEntity sender = context.player();
                    BlockPos suppliesPos = payload.suppliesPos();
                    BlockEntity maybeSupplies = sender.getWorld().getBlockEntity(suppliesPos);

                    if(maybeSupplies instanceof CampBlockEntity supplies) {
                        if(!supplies.saveStructure(payload.structName(), payload.origin(), payload.size()))
                            System.out.println("Structure failed to save");
                    } else {
                        //TODO add dimension/world checking. Might already be handled by sender.getWorld()
                        throw(new NullPointerException("Given location does not contain camp supplies! [SAV]"));
                    }
                });

        ServerPlayNetworking.registerGlobalReceiver(CampBlockBuildPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();
                    BlockPos suppliesPos = payload.suppliesPos();
                    BlockEntity maybeSupplies = sender.getWorld().getBlockEntity(suppliesPos);

                    if(maybeSupplies instanceof CampBlockEntity supplies) {
                        if(!supplies.placeStructure((ServerWorld) sender.getWorld(), payload.structureName(), payload.origin()))
                            System.out.println("Structure failed to place");
                    } else {
                        //TODO add dimension/world checking. Might already be handled by sender.getWorld()
                        throw(new NullPointerException("Given location does not contain camp supplies! [BLD]"));
                    }
                });

        ServerPlayNetworking.registerGlobalReceiver(QueryStructuresPayload.ID,
                (payload, context) -> {
                    structureDirectory = context.player().server
                            .getSavePath(WorldSavePath.GENERATED)
                            .resolve(MOD_ID)
                            //TODO reimplement each player having their own structure directory
                            //.resolve(context.player().getNameForScoreboard().toLowerCase());
                            .resolve("structures");
                    getKnownStructuresFromFile(context.player());
                });
        // endregion NETWORKING
	}

    // region HELPER METHODS
    public void getKnownStructuresFromFile(ServerPlayerEntity player) {
        try(Stream<Path> files = Files.list(structureDirectory)) {
            List<Path> list = files.filter(Files::isRegularFile).toList();
            ArrayList<String> structureNames = new ArrayList<>();
            for(Path file : list) {
                structureNames.add(file.subpath(file.getNameCount() - 1, file.getNameCount()).toString());
            }
            ServerPlayNetworking.send(player, new ReturnStructuresPayload(structureNames));
        } catch(IOException e) {
            //IDK man
        }
    }
    // endregion HELPER METHODS
}