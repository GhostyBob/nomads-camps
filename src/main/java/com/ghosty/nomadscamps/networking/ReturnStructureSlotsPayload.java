package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import com.ghosty.nomadscamps.StructureSlot;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Objects;

public record ReturnStructureSlotsPayload(ArrayList<StructureSlot> slots) implements CustomPayload {
    public static final Identifier RETURN_STRUCTURE_SLOTS_ID = Identifier.of(NomadsCamps.MOD_ID, "return_structure_slots");
    public static final CustomPayload.Id<ReturnStructureSlotsPayload> ID = new CustomPayload.Id<>(RETURN_STRUCTURE_SLOTS_ID);
    // TODO rewrite this codec (and the StructureSlot constructor) to use BlockBox's built in packetCodec
    public static final PacketCodec<RegistryByteBuf, ReturnStructureSlotsPayload> CODEC = PacketCodec.of(
            //Logic for writing
            (value, buf) -> {
                //store necessary data using buf.writeWhatever(value.data); etc.
                // Like with the old system, we have to break each StructureSlot down into primitives
                buf.writeVarInt(value.slots.size());
                for(StructureSlot slot : value.slots)
                {
                    buf.writeString(slot.getStructureName());
                    buf.writeBlockPos((slot.isPlaced()) ? Objects.requireNonNull(slot.getOccupiedArea()).getCenter() : null);
                    buf.writeInt(slot.sizeX());
                    buf.writeInt(slot.sizeY());
                    buf.writeInt(slot.sizeZ());
                    buf.writeBoolean(slot.canCaptureEntities());
                }
            },
            //Logic for reading
            buf -> {
                //read all that necessary data using buf.readWhatever(), etc.
                // and rebuild the list
                ArrayList<StructureSlot> output = new ArrayList<>();
                for(int i = buf.readVarInt(); i > 0; i--)
                {
                    output.add(new StructureSlot(
                            buf.readString(),
                            buf.readBlockPos(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean()
                    ));
                }

                return new ReturnStructureSlotsPayload(output);
            });

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
