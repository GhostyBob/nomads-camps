package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CampBlockRemovePayload(BlockPos min, BlockPos max) implements CustomPayload {
    public static final Identifier REMOVE_STRUCTURE_ID = Identifier.of(NomadsCamps.MOD_ID, "remove_camp_structure");
    public static final CustomPayload.Id<CampBlockRemovePayload> ID = new CustomPayload.Id<>(REMOVE_STRUCTURE_ID);
    public static final PacketCodec<RegistryByteBuf, CampBlockRemovePayload> CODEC = PacketCodec.of(
            //Logic for writing
            (value, buf) -> {
                //store necessary data using buf.writeWhatever(value.data); etc.
                buf.writeBlockPos(value.min);
                buf.writeBlockPos(value.max);
            },
            //Logic for reading
            buf -> new CampBlockRemovePayload(
                //read all that necessary data using buf.readWhatever(), etc.
                    buf.readBlockPos(), buf.readBlockPos())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
