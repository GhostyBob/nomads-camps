package com.ghosty.nomadscamps.util;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "nomads-camps-config", wrapperName = "NomadsCampsConfig")
public class NomadsCampsConfigModel {
    // Add fields here AND THEN BUILD THE PROJECT to add fields to the .config file.

    public int startingSlotCount = 4;

    public int startingSlotSizeX = 4;
    public int startingSlotSizeY = 4;
    public int startingSlotSizeZ = 4;

    public int maxPlacementOffset = 8;

    public boolean slotsCaptureEntities = false;
}
