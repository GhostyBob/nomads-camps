package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record CampSuppliesGUIPayload(UUID uuid) implements CustomPayload {
    public static final Identifier CAMP_SUPPLIES_GUI_ID = Identifier.of(NomadsCamps.MOD_ID, "show_camp_gui");
    public static final CustomPayload.Id<CampSuppliesGUIPayload> ID = new CustomPayload.Id<>(CAMP_SUPPLIES_GUI_ID);
    public static final PacketCodec<RegistryByteBuf, CampSuppliesGUIPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.uuid);
            }, buf -> new CampSuppliesGUIPayload(
                buf.readUuid()
            ));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
