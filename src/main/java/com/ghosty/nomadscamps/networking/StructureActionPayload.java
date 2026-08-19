/// @Author GhostyBob
/// @Version 8/18/26

package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import com.ghosty.nomadscamps.StructureSlot;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/// The payload used when a client requests the server do something with a structure.
///
/// @param type   Dictates which action will be taken. 1 is "build", 2 is "remove".
/// @param slot   The structure slot being placed, removed, etc.
/// @param position The world position to place the slot at. Unused when removing a structure.
public record StructureActionPayload(int type, StructureSlot slot, BlockPos position) implements CustomPayload {
    public static final Identifier STRUCTURE_ACTION_ID = Identifier.of(NomadsCamps.MOD_ID, "structure_placement_action");
    public static final CustomPayload.Id<StructureActionPayload> ID = new CustomPayload.Id<>(STRUCTURE_ACTION_ID);
    public static final PacketCodec<RegistryByteBuf, StructureActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, StructureActionPayload::type,
                    StructureSlot.PACKET_CODEC, StructureActionPayload::slot,
                    BlockPos.PACKET_CODEC, StructureActionPayload::position,
                    StructureActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
