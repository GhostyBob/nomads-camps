package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record CampBlockSavePayload(BlockPos suppliesPos, Identifier structName, BlockPos origin, Vec3d size) implements CustomPayload {
    public static final Identifier SET_CAMP_BLOCK_ID = Identifier.of(NomadsCamps.MOD_ID, "update_camp_block");
    public static final CustomPayload.Id<CampBlockSavePayload> ID = new CustomPayload.Id<>(SET_CAMP_BLOCK_ID);
    public static final PacketCodec<RegistryByteBuf, CampBlockSavePayload> CODEC = PacketCodec.of(
            //Logic for writing
            (value, buf) -> {
                //store necessary data using buf.writeWhatever(value.data); etc.
                buf.writeBlockPos(value.suppliesPos);
                buf.writeIdentifier(value.structName);
                buf.writeBlockPos(value.origin);
                buf.writeVec3d(value.size);
            },
            //Logic for reading
            buf -> new CampBlockSavePayload(
                    //read all that necessary data using buf.readWhatever(), etc.
                    buf.readBlockPos(), buf.readIdentifier(), buf.readBlockPos(), buf.readVec3d()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
