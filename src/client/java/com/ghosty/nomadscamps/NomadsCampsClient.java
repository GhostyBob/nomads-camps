package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.ReturnSlotsPayload;
import com.ghosty.nomadscamps.networking.ShowGUIPayload;
import com.ghosty.nomadscamps.networking.UpdateSlotsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class NomadsCampsClient implements ClientModInitializer {
	// region FIELDS
    private ArrayList<StructureSlot> slots;

    public @Nullable ArrayList<StructureSlot> getSlots() {
        return slots;
    }

    public static NomadsCampsClient instance;
    // endregion FIELDS

    @Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
        instance = this;

        // region NETWORKING
        ClientPlayNetworking.registerGlobalReceiver(ShowGUIPayload.ID, (payload, context) -> {
            CampSuppliesGUI gui = new CampSuppliesGUI(payload.showClaimScreen() ? "claim" : "home");

            MinecraftClient.getInstance().setScreen(gui);
        });

        ClientPlayNetworking.registerGlobalReceiver(ReturnSlotsPayload.ID, (payload, context) -> {
            slots = payload.slots();
        });
        // endregion NETWORKING
	}
}