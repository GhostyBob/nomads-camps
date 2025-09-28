package com.ghosty.nomadscamps.util;

import com.ghosty.nomadscamps.CampBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SuppliesData {
//    public static @Nullable UUID getOwnedSupplies(IEntityDataSaver player) {
//        NbtCompound nbt = player.getPersistentData();
//
//        try {
//            return nbt.getUuid("supplies_uuid");
//        }
//        catch(NullPointerException e) {
//            return null;
//        }
//    }

//    public static void setOwnedSupplies(IEntityDataSaver player, UUID suppliesUuid) {
//        NbtCompound nbt = player.getPersistentData();
//
//        nbt.putUuid("supplies_uuid", suppliesUuid);
//    }

    public static void setOwnedSupplies(IEntityDataSaver player) {
        NbtCompound nbt = player.getPersistentData();
        nbt.putBoolean("owns_supplies", true);
    }

    public static @Nullable BlockPos getOwnedSuppliesPos(IEntityDataSaver player) {
        NbtCompound nbt = player.getPersistentData();

        //TODO add better checking for if suppliespos is null
        if(nbt.getLong("supplies_pos") == 0L)
            return null;

        //TODO add dimension/world checking

        return BlockPos.fromLong(nbt.getLong("supplies_pos"));
    }

    public static boolean setOwnedSuppliesPos(IEntityDataSaver player, BlockPos pos) {
        NbtCompound nbt = player.getPersistentData();

        nbt.putLong("supplies_pos", pos.asLong());

        return true;
    }

    public static boolean bindSupplies(ServerPlayerEntity player, CampBlockEntity supplies) {
        NbtCompound playerNbt = ((IEntityDataSaver) player).getPersistentData();
        NbtCompound suppliesNbt = new NbtCompound();
        supplies.writeNbt(suppliesNbt, player.getWorld().getRegistryManager());

        playerNbt.putUuid("supplies_uuid", suppliesNbt.getUuid("uuid"));
        return true;
    }
}
