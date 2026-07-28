package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import com.ghosty.nomadscamps.StructureSlot;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public record UpdateSlotsPayload(boolean changed, ArrayList<StructureSlot> slots) implements CustomPayload {
    public static final Identifier UPDATE_SLOTS_ID = Identifier.of(NomadsCamps.MOD_ID, "update_slot_data");
    public static final CustomPayload.Id<UpdateSlotsPayload> ID = new CustomPayload.Id<>(UPDATE_SLOTS_ID);
    public static final PacketCodec<RegistryByteBuf, UpdateSlotsPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL, UpdateSlotsPayload::changed,
                    PacketCodecs.collection(
                            ArrayList::new, StructureSlot.PACKET_CODEC
                    ),
                    UpdateSlotsPayload::slots,
                    UpdateSlotsPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}