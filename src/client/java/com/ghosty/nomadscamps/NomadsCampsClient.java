package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.CampSuppliesGUIPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

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
	}

    public static String[] getKnownStructuresFromFile() {
        //TODO Implement this method
        return new String[0];
    }
}