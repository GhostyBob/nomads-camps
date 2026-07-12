package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;

public record ReturnStructuresPayload(ArrayList<String> names) implements CustomPayload {
    public static final Identifier RETURN_STRUCTURES_ID = Identifier.of(NomadsCamps.MOD_ID, "return_saved_structures");
    public static final CustomPayload.Id<ReturnStructuresPayload> ID = new CustomPayload.Id<>(RETURN_STRUCTURES_ID);
    public static final PacketCodec<RegistryByteBuf, ReturnStructuresPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                // We have to write a bunch of strings into the packet manually
                // since there's no buf.writeArrayList.
                buf.writeVarInt(value.names().size());
                for(String name : value.names()) {
                    buf.writeString(name);
                }
            }, buf -> {
                // That means we also have to build an ArrayList out ouf the individual strings
                int size = buf.readVarInt();
                ArrayList<String> returnedNames = new ArrayList<>();
                for(int i = 0; i < size; i++) {
                    returnedNames.add(buf.readString());
                }
                return new ReturnStructuresPayload(returnedNames);
            });

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    //private ArrayList<String> names() {}
}
