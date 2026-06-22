package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public record CampBlockBuildPayload(BlockPos suppliesPos, Identifier structureName, BlockPos origin) implements CustomPayload {
    public static final Identifier CAMP_BLOCK_BUILD_ID = Identifier.of(NomadsCamps.MOD_ID, "camp_block_build");
    public static final CustomPayload.Id<CampBlockBuildPayload> ID = new CustomPayload.Id<>(CAMP_BLOCK_BUILD_ID);
    public static final PacketCodec<RegistryByteBuf, CampBlockBuildPayload> CODEC = PacketCodec.of(
            //Logic for writing
            (value, buf) -> {
                //store necessary data using buf.writeWhatever(value.data); etc.
                buf.writeBlockPos(value.suppliesPos);
                buf.writeIdentifier(value.structureName);
                buf.writeBlockPos(value.origin);
            },
            //Logic for reading
            buf -> new CampBlockBuildPayload(
                    //read all that necessary data using buf.readWhatever(), etc.
                    buf.readBlockPos(), buf.readIdentifier(), buf.readBlockPos()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
