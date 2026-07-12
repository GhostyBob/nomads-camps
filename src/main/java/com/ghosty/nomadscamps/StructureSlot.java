package com.ghosty.nomadscamps;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class StructureSlot {
    // TODO make these pull from the .config file
    // TODO also set up a .config file

    // region FIELDS
    private final String structureName;

    @Nullable
    private BlockBox occupiedArea;

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private boolean isPlaced;

    private final boolean captureEntities;
    // private List ignoredBlocks;
    // endregion FIELDS

    // region CONSTRUCTORS
    public StructureSlot() {
        occupiedArea = null;
        isPlaced = false;

        sizeX = 4;
        sizeY = 4;
        sizeZ = 4;

        structureName = "Empty Slot";

        captureEntities = false;
    }

    public StructureSlot(String name, @Nullable BlockPos center, int sizeX, int sizeY, int sizeZ, boolean captureEntities) {
        structureName = name;

        isPlaced = center != null;
        if (isPlaced) {
            // TODO scrutinize these formulae a bit more
            occupiedArea = new BlockBox(
                    // min = center - ((size + 1) / 2)
                    center.getX() - ((sizeX + 1) / 2),
                    center.getY() - ((sizeY + 1) / 2),
                    center.getZ() - ((sizeZ + 1) / 2),
                    // max = center - ((3size + 1) / 2)
                    center.getX() - ((sizeX + 1) / 2) + sizeX,
                    center.getY() - ((sizeY + 1) / 2) + sizeY,
                    center.getZ() - ((sizeZ + 1) / 2) + sizeZ
                    );
        } else occupiedArea = null;

        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;

        this.captureEntities = captureEntities;
    }
    // endregion CONSTRUCTORS

    // region GETTERS
    public @Nullable BlockBox getOccupiedArea() { return occupiedArea; }

    public boolean isPlaced() { return isPlaced; }

    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }

    public String getStructureName() { return structureName; }

    public boolean canCaptureEntities() { return captureEntities; }
    // endregion GETTERS

    // region METHODS
    public void Place(BlockPos minCorner) {
        isPlaced = true;

        occupiedArea = new BlockBox(
                minCorner.getX(),
                minCorner.getY(),
                minCorner.getZ(),
                minCorner.getX() + sizeX,
                minCorner.getY() + sizeY,
                minCorner.getZ() + sizeZ
        );
    }

    public void Remove() {
        isPlaced = false;

        occupiedArea = null;
    }
    // endregion METHODS
}
