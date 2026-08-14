/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import com.ghosty.nomadscamps.StructureSlot;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

/// The payload used when sending a list of structure slots from the server to the client.
///
/// @param slots The list of slots being sent. Can either be the full list when initializing the
///              client, or a list of "dirty" slots that must be updated on the client.
public record ReturnSlotsPayload(ArrayList<StructureSlot> slots) implements CustomPayload {
    public static final Identifier RETURN_SLOTS_ID = Identifier.of(NomadsCamps.MOD_ID, "return_slot_data");
    public static final CustomPayload.Id<ReturnSlotsPayload> ID = new CustomPayload.Id<>(RETURN_SLOTS_ID);
    public static final PacketCodec<RegistryByteBuf, ReturnSlotsPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.collection(
                            ArrayList::new, StructureSlot.PACKET_CODEC
                    ),
                    ReturnSlotsPayload::slots,
                    ReturnSlotsPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}