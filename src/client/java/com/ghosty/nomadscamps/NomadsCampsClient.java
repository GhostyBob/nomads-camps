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

        // region NETWORKING
        ClientPlayNetworking.registerGlobalReceiver(CampSuppliesGUIPayload.ID, (payload, context) -> {
            CampSuppliesGUI gui = new CampSuppliesGUI(payload.showClaimScreen() ? "claim" : "home", payload.pos());

            MinecraftClient.getInstance().setScreen(gui);
        });

        ClientPlayNetworking.registerGlobalReceiver(ReturnStructuresPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if(context.client().currentScreen instanceof CampSuppliesGUI gui) {
                    if(gui.address.equals("structureList")) {
                        gui.receiveStructures(payload.names());
                    }
                }
            });
        });
        // endregion NETWORKING
	}
}