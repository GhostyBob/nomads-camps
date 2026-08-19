/// @author GhostyBob
/// @version 8/18/26

package com.ghosty.nomadscamps.networking;

import com.ghosty.nomadscamps.NomadsCamps;
import com.ghosty.nomadscamps.StructureSlot;
import com.ghosty.nomadscamps.UpgradeTracker;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

/// The payload used when sending an UpgradeTracker from the server to the client.
///
/// @param tracker The UpgradeTracker object being sent.
public record ReturnUpgradeTrackerPayload(UpgradeTracker tracker) implements CustomPayload {
    public static final Identifier RETURN_TRACKER_ID = Identifier.of(NomadsCamps.MOD_ID, "return_upgrade_data");
    public static final CustomPayload.Id<ReturnUpgradeTrackerPayload> ID = new CustomPayload.Id<>(RETURN_TRACKER_ID);
    public static final PacketCodec<RegistryByteBuf, ReturnUpgradeTrackerPayload> CODEC =
            PacketCodec.tuple(
                    UpgradeTracker.PACKET_CODEC, ReturnUpgradeTrackerPayload::tracker,
                    ReturnUpgradeTrackerPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
