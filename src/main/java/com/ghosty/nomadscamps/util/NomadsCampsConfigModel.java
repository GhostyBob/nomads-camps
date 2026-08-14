/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps.util;

import io.wispforest.owo.config.annotation.Config;

/// The structure of the .config file used by this mod.
/// Must be packed into a wrapper class (named NomadsCampsConfig) to be
/// used, so remember to build the project after changing this class.
@Config(name = "nomads-camps-config", wrapperName = "NomadsCampsConfig")
public class NomadsCampsConfigModel {
    /// The number of structures slots players have when they first craft
    /// and interact with Camp Supplies.
    public int startingSlotCount = 4;

    // The size of the area that a single structure slot can "capture" by default.
    public int startingSlotSizeX = 4;
    public int startingSlotSizeY = 4;
    public int startingSlotSizeZ = 4;

    ///  The furthest in any direction that a structure can be offset from
    /// its Camp Supplies while being placed.
    public int maxPlacementOffset = 8;

    /// Whether structure slots can store entities when being packed.
    public boolean slotsCaptureEntities = false;
}
