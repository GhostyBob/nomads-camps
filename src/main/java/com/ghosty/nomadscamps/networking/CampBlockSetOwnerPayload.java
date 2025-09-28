package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CampBlockSetOwnerPayload() implements CustomPayload {
    public static final Identifier SET_CAMP_BLOCK_OWNER = Identifier.of(NomadsCamps.MOD_ID, "set_supplies_owner");
    public static final CustomPayload.Id<CampBlockSetOwnerPayload> ID = new CustomPayload.Id(SET_CAMP_BLOCK_OWNER);
    public static final PacketCodec<RegistryByteBuf, CampBlockSetOwnerPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
            },
            buf -> new CampBlockSetOwnerPayload(
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
