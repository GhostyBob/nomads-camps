package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record QueryStructuresPayload(BlockPos suppliesPos) implements CustomPayload{
    public static final Identifier QUERY_STRUCTURES_ID = Identifier.of(NomadsCamps.MOD_ID, "query_saved_structures");
    public static final CustomPayload.Id<QueryStructuresPayload> ID = new CustomPayload.Id<>(QUERY_STRUCTURES_ID);
    public static final PacketCodec<RegistryByteBuf, QueryStructuresPayload> CODEC = PacketCodec.of(
            //Logic for writing
            (value, buf) -> {
                //store necessary data using buf.writeWhatever(value.data); etc.
                buf.writeBlockPos(value.suppliesPos);
            },
            //Logic for reading
            buf -> new QueryStructuresPayload(
                    //read all that necessary data using buf.readWhatever(), etc.
                    buf.readBlockPos()
            ));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
