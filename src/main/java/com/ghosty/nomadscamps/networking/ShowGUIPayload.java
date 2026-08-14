/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/// The payload used when telling a client to display the camp supplies GUI.
///
/// @param suppliesPos The position of the camp supplies block that the player interacted
///                    with. Necessary when placing structures to convert from an offset
///                    to a world position.
public record ShowGUIPayload(BlockPos suppliesPos) implements CustomPayload {
    public static final Identifier SHOW_GUI_ID = Identifier.of(NomadsCamps.MOD_ID, "show_camp_gui");
    public static final CustomPayload.Id<ShowGUIPayload> ID = new CustomPayload.Id<>(SHOW_GUI_ID);
    public static final PacketCodec<RegistryByteBuf, ShowGUIPayload> CODEC = PacketCodec.of(
            (value, buf) ->
                    buf.writeBlockPos(value.suppliesPos),
            buf -> new ShowGUIPayload(
                    buf.readBlockPos()
            ));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
