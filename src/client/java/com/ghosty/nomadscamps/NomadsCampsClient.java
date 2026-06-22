package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.CampSuppliesGUIPayload;
import com.ghosty.nomadscamps.networking.ReturnStructuresPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.structure.StructureTemplateManager;

public class NomadsCampsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.


        //NETWORKING
        //How to handle the payload telling the client to open the camp supplies GUI
        ClientPlayNetworking.registerGlobalReceiver(CampSuppliesGUIPayload.ID, (payload, context) -> {
            CampSuppliesGUI gui = new CampSuppliesGUI(payload.showClaimScreen() ? "claim" : "home", payload.pos());

            MinecraftClient.getInstance().setScreen(gui);
        });

        //How to handle the payload telling the client the names of its registered structures
        ClientPlayNetworking.registerGlobalReceiver(ReturnStructuresPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if(context.client().currentScreen instanceof CampSuppliesGUI gui) {
                    if(gui.address.equals("structureList")) {
                        gui.receiveStructures(payload.names());
                    }
                }
            });
        });
	}
}