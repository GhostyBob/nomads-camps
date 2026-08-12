package com.ghosty.nomadscamps.mixin;

import net.minecraft.structure.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/// An accessor mixin whose sole purpose is to resolve one access error in TaggedStructureTemplateMixin.
@Mixin(StructureTemplate.PalettedBlockInfoList.class)
public interface PalettedBlockInfoListAccessorMixin {
    @Invoker("<init>")
    static StructureTemplate.PalettedBlockInfoList create(List<StructureTemplate.StructureBlockInfo> infos) {
        throw new AssertionError("Implemented in a mixin.");
    }
}
