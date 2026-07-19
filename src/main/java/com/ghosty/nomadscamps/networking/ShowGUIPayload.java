package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ShowGUIPayload(boolean showClaimScreen) implements CustomPayload {
    public static final Identifier SHOW_GUI_ID = Identifier.of(NomadsCamps.MOD_ID, "show_camp_gui");
    public static final CustomPayload.Id<ShowGUIPayload> ID = new CustomPayload.Id<>(SHOW_GUI_ID);
    public static final PacketCodec<RegistryByteBuf, ShowGUIPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBoolean(value.showClaimScreen);
            }, buf -> new ShowGUIPayload(
                    buf.readBoolean()
            ));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
