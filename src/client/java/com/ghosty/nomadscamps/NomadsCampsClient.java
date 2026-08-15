/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.ReturnSlotsPayload;
import com.ghosty.nomadscamps.networking.ShowGUIPayload;
import com.ghosty.nomadscamps.networking.StructureActionPayload;
import com.ghosty.nomadscamps.networking.UpdateSlotsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/// Controls the clientside logic of the mod. In charge of networking.
/// Also keeps a list of this client's structure slots for easy access.
public class NomadsCampsClient implements ClientModInitializer {
    // region FIELDS
    /// The clientside list of structure slots. Shouldn't be set
    /// outside this class.
    private ArrayList<StructureSlot> slots;

    /// Getter for the clientside slot list.
    public @Nullable ArrayList<StructureSlot> getSlots() {
        return slots;
    }

    /// Allows access to the clientside slot list from a static context.
    public static NomadsCampsClient instance;
    // endregion FIELDS

    /// The client-specific entrypoint for the mod. Defines server-bound networking
    /// payload handlers
    @Override
    public void onInitializeClient() {
        instance = this;

        // Handler for the Show GUI Payload
        // Creates a CampSuppliesGUI set to the structure list and displays it.
        ClientPlayNetworking.registerGlobalReceiver(ShowGUIPayload.ID, (payload, context) -> {
            CampSuppliesGUI gui = new CampSuppliesGUI("structureList", payload.suppliesPos());
            MinecraftClient.getInstance().setScreen(gui);
        });

        // Handler for the Return Slots Payload
        ClientPlayNetworking.registerGlobalReceiver(ReturnSlotsPayload.ID, (payload, context) -> {
            // If the client's slots haven't been initialized yet, this
            // should be the full list of slots.
            if (slots == null) {
                slots = payload.slots();
                // If the client's slots are already initialized, we should have just received
                // a list of dirty slots instead that need to be updated.
            } else {
                for (StructureSlot newSlot : payload.slots()) {
                    this.slots.set(newSlot.getIndex(), newSlot);
                }
                // Once the changes have been incorporated, return the new list of slots to be saved to file.
                ClientPlayNetworking.send(new UpdateSlotsPayload(true, slots));
            }
        });
    }

    // region HELPER METHODS

    /// Sends an Update Slots Payload to the server, requesting the full list
    /// of structure slots be sent back.
    public static void sendQueryStructuresPacket() {
        ClientPlayNetworking.send(new UpdateSlotsPayload(false, new ArrayList<>()));
    }

    /// Sends a Structure Action Payload to the server, requesting the given
    /// slot be placed at the given position.
    public static void sendBuildPacket(StructureSlot slot, BlockPos origin) {
        ClientPlayNetworking.send(new StructureActionPayload(1, slot, origin));
    }

    /// Sends a Structure Action Payload to the server, requesting the given
    /// slot be removed from the world.
    public static void sendRemovePacket(StructureSlot slot) {
        ClientPlayNetworking.send(new StructureActionPayload(2, slot, BlockPos.ORIGIN));
    }
    // endregion HELPER METHODS
}