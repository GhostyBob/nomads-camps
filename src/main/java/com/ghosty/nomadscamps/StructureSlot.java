/// @Author GhostyBob
/// @Version 8/18/26

package com.ghosty.nomadscamps;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Holds all the data associated with a Camp Supplies structure
public class StructureSlot {
    /// The human-readable name of the structure. Displayed in the camp supplies GUI.
    public String structureName;
    /// The machine-readable name of the structure. Used as a unique identifier for
    /// the structure and when saving to file.
    public Identifier structureFileName;
    /// This slot's index within the structure list. Used to keep slots straight
    /// when updating name and filename data.
    private final int index;

    /// The area occupied by the structure slot. Null if the structure isn't currently
    /// placed.
    @Nullable
    private BlockBox occupiedArea;
    /// The max structure size for this slot along the x-axis.
    private int sizeX;
    /// The max structure size for this slot along the y-axis.
    private int sizeY;
    /// The max structure size for this slot along the z-axis.
    private int sizeZ;
    /// Whether the structure is currently placed in the world.
    private boolean isPlaced;
    /// Whether the structure should "capture" the entities in the structure's area
    /// when it is packed.
    private final boolean captureEntities;

    /// A PacketCodec telling the game how to write the data of a StructureSlot into a
    /// networking packet, as well as how to turn a networking packet back into a
    /// StructureSlot object.
    // region CODEC DEFINITION
    public static final PacketCodec<RegistryByteBuf, StructureSlot> PACKET_CODEC =
            PacketCodec.of((value, buf) -> {
                buf.writeString(value.structureName);
                buf.writeString(value.structureFileName.toString());
                buf.writeInt(value.index);
                buf.writeNullable(
                        value.isPlaced() ?
                                new BlockPos(
                                        Objects.requireNonNull(value.getOccupiedArea()).getMinX(),
                                        Objects.requireNonNull(value.getOccupiedArea()).getMinY(),
                                        Objects.requireNonNull(value.getOccupiedArea()).getMinZ()) :
                                null,
                        (buf1, value1) -> buf1.writeBlockPos(value1)
                );
                buf.writeInt(value.sizeX());
                buf.writeInt(value.sizeY());
                buf.writeInt(value.sizeZ());
                buf.writeBoolean(value.canCaptureEntities());
            }, buf -> new StructureSlot(
                    buf.readString(),
                    Identifier.of(buf.readString()),
                    buf.readInt(),
                    buf.readNullable(buf1 -> buf1.readBlockPos()),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean()
            ));
    // endregion CODEC DEFINITION

    /// Constructs a StructureSlot with the default values (pulled from the .config)
    /// and the given index.
    ///
    /// @param index This slot's position within the structure list.
    public StructureSlot(int index) {
        occupiedArea = null;
        isPlaced = false;

        sizeX = NomadsCamps.CONFIG.startingSlotSizeX();
        sizeY = NomadsCamps.CONFIG.startingSlotSizeY();
        sizeZ = NomadsCamps.CONFIG.startingSlotSizeZ();

        structureName = "Empty Slot";
        structureFileName = NomadsCamps.DEFAULT_STRUCTURE_FILENAME;
        this.index = index;

        captureEntities = NomadsCamps.CONFIG.slotsCaptureEntities();
    }

    /// Constructs a StructureSlot by manually setting all of its fields.
    /// Should only be used by the codec defined above.
    private StructureSlot(String name, Identifier fileName, int index, @Nullable BlockPos min, int sizeX, int sizeY, int sizeZ, boolean captureEntities) {
        structureName = name;
        structureFileName = fileName;
        this.index = index;

        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;

        isPlaced = min != null;
        if (isPlaced) {
            occupiedArea = getProposedArea(min);
        } else occupiedArea = null;

        this.captureEntities = captureEntities;
    }

    /// @return This slot's index within the structure slot list.
    public int getIndex() {
        return index;
    }

    /// @return A BlockBox representing the area occupied by this structure
    /// slot, or null if this slot isn't placed in the world.
    public @Nullable BlockBox getOccupiedArea() {
        return occupiedArea;
    }

    /// @return True if this structure is currently placed in the world;
    /// false otherwise.
    public boolean isPlaced() {
        return isPlaced;
    }

    /// @return The max structure size for this slot along the x-axis.
    public int sizeX() {
        return sizeX;
    }

    /// Sets sizeX to the specified amount.
    ///
    /// @param size The new sizeX
    public void setSizeX(int size) {
        sizeX = size;
    }

    /// @return The max structure size for this slot along the y-axis
    public int sizeY() {
        return sizeY;
    }

    /// Sets sizeY to the specified amount.
    ///
    /// @param size The new sizeY
    public void setSizeY(int size) {
        sizeY = size;
    }

    /// @return The max structure size for this slot along the z-axis.
    public int sizeZ() {
        return sizeZ;
    }

    /// Sets sizeZ to the specified amount.
    ///
    /// @param size The new sizeZ
    public void setSizeZ(int size) {
        sizeZ = size;
    }

    /// @return Whether this structure should store entities within its
    /// occupiedArea when being saved and packed.
    public boolean canCaptureEntities() {
        return captureEntities;
    }

    /// Updates the data of this structure slot to represent it being placed
    /// in the world.
    ///
    /// @param minCorner The location that this slot's structure was placed at.
    ///                                   Anchored to the (-x, -y, -z) corner.
    public void place(BlockPos minCorner) {
        isPlaced = true;
        occupiedArea = getProposedArea(minCorner);
    }

    /// Updates the data of this structure slot to represent it being removed
    /// from the world.
    public void remove() {
        isPlaced = false;
        occupiedArea = null;
    }

    /// Calculates the area this slot's structure would occupy if it was placed
    /// at the given position.
    ///
    /// @param origin The position to anchor the simulated structure to. Anchored
    ///               to the (-x, -y, -z) corner.
    /// @return A BlockBox representing what this slot's occupiedArea would be if
    /// place() was called with the passed position.
    public BlockBox getProposedArea(BlockPos origin) {
        return new BlockBox(
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                origin.getX() + sizeX - 1,
                origin.getY() + sizeY - 1,
                origin.getZ() + sizeZ - 1
        );
    }
}
