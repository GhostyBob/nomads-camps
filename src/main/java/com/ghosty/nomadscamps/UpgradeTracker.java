package com.ghosty.nomadscamps;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/// A data structure to track camp supply upgrades that can easily be
/// serialized into a .json file.
public class UpgradeTracker {
    /// The number of structure slot size upgrades that the player
    /// has installed but not yet allocated.
    public int unusedSlotSizeUpgrades;
    /// The number of structure slot count upgrades that the
    /// player has installed but not yet used.
    public int unusedSlotCountUpgrades;

    /// A PacketCodec definition so the base game can serialize and
    /// deserialize this class for networking purposes.
    public static final PacketCodec<RegistryByteBuf, UpgradeTracker> PACKET_CODEC =
            PacketCodec.of((value, buf) -> {
                buf.writeInt(value.unusedSlotSizeUpgrades);
                buf.writeInt(value.unusedSlotCountUpgrades);
            }, buf -> new UpgradeTracker(
                    buf.readInt(),
                    buf.readInt()
            ));

    /// Constructs an UpgradeTracker using default values pulled from the .config.
    public UpgradeTracker() {
        unusedSlotSizeUpgrades = NomadsCamps.CONFIG.startingSlotSizeUpgrades();
        unusedSlotCountUpgrades = NomadsCamps.CONFIG.startingSlotCountUpgrades();
    }

    /// Constructs an UpgradeTracker with the specified values. Should only be
    /// used by the PacketCodec defined above.
    /// @param size The value to be stored in unusedSlotSizeUpgrades.
    /// @param count The value to be stored in unusedSlotCountUpgrades.
    private UpgradeTracker(int size, int count) {
        unusedSlotSizeUpgrades = size;
        unusedSlotCountUpgrades = count;
    }
}
