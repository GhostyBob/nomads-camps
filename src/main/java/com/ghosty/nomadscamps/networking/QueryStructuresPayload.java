package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record QueryStructuresPayload() implements CustomPayload{
    public static final Identifier QUERY_STRUCTURES_ID = Identifier.of(NomadsCamps.MOD_ID, "query_saved_structures");
    public static final CustomPayload.Id<QueryStructuresPayload> ID = new CustomPayload.Id<>(QUERY_STRUCTURES_ID);
    public static final PacketCodec<RegistryByteBuf, QueryStructuresPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
            }, buf -> new QueryStructuresPayload(
            ));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
