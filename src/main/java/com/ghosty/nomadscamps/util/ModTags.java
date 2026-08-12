package com.ghosty.nomadscamps.util;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> PACKING_IGNORED_BLOCKS = createTag("packing_ignored_blocks");

        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(NomadsCamps.MOD_ID, name));
        }
    }
}
