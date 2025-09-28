package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.CampSuppliesGUIPayload;
import com.ghosty.nomadscamps.util.IEntityDataSaver;
import com.ghosty.nomadscamps.util.SuppliesData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CampBlockEntity extends BlockEntity {
    private PlayerEntity owner;
    private UUID uuid;

    //Constructor
    public CampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMP_BLOCK_ENTITY, pos, state);
    }

    //Put custom block methods below
    public void showGUI(PlayerEntity player) {
        //Get the relevant ServerPlayerEntity from the passed PlayerEntity
        if(player instanceof ServerPlayerEntity serverPlayer) {
            if(checkOwner(player)) {
                //Send a packet to the client to open the camp supplies GUI
                ServerPlayNetworking.send(serverPlayer, new CampSuppliesGUIPayload(this.uuid));
            } else {
                serverPlayer.sendMessage(Text.of("You don't own these supplies!"), true);
            }
        }
    }

//    public boolean setOwner(PlayerEntity player) {
//        if(owner == null) {
//            owner = player;
//            return true;
//        }
//        return false;
//    }

    public boolean setOwner(PlayerEntity player) {
        if(uuid == null) {
            uuid = player.getUuid();
            return true;
        }
        return false;
    }

    public boolean checkOwner(PlayerEntity player) {
        if(owner == null) return false;
        return uuid.equals(player.getUuid());
    }

    public PlayerEntity getOwner() { return owner; }

    //Functionality related to saving/placing structures
    private boolean ableToSave = true;
    //private String structureName;
    private Identifier templateName;

    public boolean saveStructure(@Nullable Identifier structureName, BlockPos origin, Vec3d structureSize) {
        if(!ableToSave || structureName == null)
            return false;
        else {
            setTemplateName(structureName);

            //convert structureSize to a Vec3i
            Vec3i structureSizeInt = new Vec3i(
                    (int) Math.floor(structureSize.x),
                    (int) Math.floor(structureSize.y),
                    (int) Math.floor(structureSize.z)
            );

            //get the world the structure is in
            ServerWorld world = (ServerWorld) this.world;

            StructureTemplateManager templateManager = world.getStructureTemplateManager();
            StructureTemplate structureTemplate;
            try {
                structureTemplate = templateManager.getTemplateOrBlank(templateName);
            } catch (InvalidIdentifierException e) {
                return false;
            }
            //Write the structure from the world to the template
            structureTemplate.saveFromWorld(world, origin, structureSizeInt, true, Blocks.BEDROCK);
            structureTemplate.setAuthor(owner.getUuidAsString());
            //Save the template
            try {
                System.out.println("Successfully saved " + structureName);
                return templateManager.saveTemplate(templateName);
            } catch (InvalidIdentifierException e) {
                return false;
            }
        }
    }

    public void setTemplateName(@Nullable String templateName) {
        this.setTemplateName(StringHelper.isEmpty(templateName) ? null : Identifier.tryParse(templateName));
    }

    public void setTemplateName(@Nullable Identifier templateName) {
        this.templateName = templateName;
    }

    public static enum Action {
        UPDATE_DATA,
        SAVE_AREA,
        LOAD_AREA,
        SCAN_AREA;
        //etc...
    }

    //Functionality related to persistent data
    public UUID getUuid() { return uuid; }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        //BlockEntity is an abstract class, so this shouldn't be necessary
        //but if something breaks, uncomment it.
        //super.writeNbt(nbt, registries);
        nbt.putUuid("uuid", uuid);
    }


    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        //super.readNbt(nbt, registries);
        if(nbt.containsUuid("uuid")) uuid = nbt.getUuid("uuid");
    }


}
