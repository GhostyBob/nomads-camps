package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SetOwnerPayload() implements CustomPayload {
    public static final Identifier SET_OWNER_ID = Identifier.of(NomadsCamps.MOD_ID, "set_supplies_owner");
    public static final CustomPayload.Id<SetOwnerPayload> ID = new CustomPayload.Id<>(SET_OWNER_ID);
    public static final PacketCodec<RegistryByteBuf, SetOwnerPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                //buf.writeBlockPos(value.suppliesPos);
            },
            buf -> new SetOwnerPayload(
                    //buf.readBlockPos()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}