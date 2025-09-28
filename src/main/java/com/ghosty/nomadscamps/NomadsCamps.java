package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.CampBlockSetOwnerPayload;
import com.ghosty.nomadscamps.networking.CampSuppliesGUIPayload;
import com.ghosty.nomadscamps.networking.CampBlockSavePayload;
import com.ghosty.nomadscamps.util.IEntityDataSaver;
import com.ghosty.nomadscamps.util.SuppliesData;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NomadsCamps implements ModInitializer {
	public static final String MOD_ID = "nomads-camps";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        ModBlocks.initialize();
        ModBlockEntities.initialize();

        //Networking
        PayloadTypeRegistry.playS2C().register(CampSuppliesGUIPayload.ID, CampSuppliesGUIPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CampBlockSavePayload.ID, CampBlockSavePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CampBlockSetOwnerPayload.ID, CampBlockSetOwnerPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CampBlockSetOwnerPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity sender = context.player();
                    BlockEntity maybeSupplies = sender.getWorld().getBlockEntity(payload.suppliesLocation());

                    if(maybeSupplies instanceof CampBlockEntity supplies) {
                        if(supplies.setOwner(sender))
                            System.out.println("Owner set!");

                        SuppliesData.setOwnedSuppliesPos((IEntityDataSaver) sender, payload.suppliesLocation());
                    } else {
                        throw(new NullPointerException("Given location does not contain camp supplies! [OWN]"));
                    }
                });

//        ServerPlayNetworking.registerGlobalReceiver(CampBlockSavePayload.ID,
//                (payload, context) -> {
//                    //TODO insert logic upon receiving payload
//                    //access payload data using payload.fieldName();
//                    ServerPlayerEntity sender = context.player();
//                    BlockPos suppliesPos = SuppliesData.getOwnedSuppliesPos((IEntityDataSaver) sender);
//                    BlockEntity maybeSupplies = sender.getWorld().getBlockEntity(suppliesPos);
//
//                    if(maybeSupplies instanceof CampBlockEntity supplies) {
//                        if(!supplies.saveStructure(payload.structName(), payload.origin(), payload.size()))
//                            System.out.println("Structure failed to save");
//                    } else {
//                        //TODO add dimension/world checking
//                        throw(new NullPointerException("Given location does not contain camp supplies! [SAV]"));
//                    }
//                });
	}
}