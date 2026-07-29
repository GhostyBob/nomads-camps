package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.ReturnSlotsPayload;
import com.ghosty.nomadscamps.networking.ShowGUIPayload;
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
            CampSuppliesGUI gui = new CampSuppliesGUI(payload.showClaimScreen() ? "claim" : "structureList");

            MinecraftClient.getInstance().setScreen(gui);
        });

        ClientPlayNetworking.registerGlobalReceiver(ReturnSlotsPayload.ID, (payload, context) -> {
            // TODO initialize client slots immediately upon startup.
            //  That or move the else part of this lambda to a different packet
            if (slots == null) {
                slots = payload.slots();
            // If the client's slots are already initialized, we should have just received
            // a list of dirty slots instead that need to be updated.
            } else {
                for(StructureSlot newSlot : payload.slots()) {
                    if(newSlot.isDirty())
                        this.slots.set(newSlot.getIndex(), newSlot);
                }
            }
        });
        // endregion NETWORKING
	}
}