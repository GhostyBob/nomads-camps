package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import com.ghosty.nomadscamps.StructureSlot;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record StructureActionPayload(int type, BlockPos pos, StructureSlot slot, BlockPos origin) implements CustomPayload {
    public static final Identifier STRUCTURE_ACTION_ID = Identifier.of(NomadsCamps.MOD_ID, "structure_placement_action");
    public static final CustomPayload.Id<StructureActionPayload> ID = new CustomPayload.Id<>(STRUCTURE_ACTION_ID);
    public static final PacketCodec<RegistryByteBuf, StructureActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, StructureActionPayload::type,
                    BlockPos.PACKET_CODEC, StructureActionPayload::pos,
                    StructureSlot.PACKET_CODEC, StructureActionPayload::slot,
                    BlockPos.PACKET_CODEC, StructureActionPayload::origin,
                    StructureActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
