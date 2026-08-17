/// @Author GhostyBob
/// @Version 8/14/26

package com.ghosty.nomadscamps.util;

import com.ghosty.nomadscamps.NomadsCamps;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/// Registers custom TagKeys.
public class ModTags {
    /// Called by the main mod class to initialize static constants.
    public static void initialize() {
        Blocks.initialize();
    }

    public static class Blocks {
        /// The list of blocks ignored when packing structures into Camp Supplies.
        /// Meant to contain blocks that are unobtainable or immovable in the base game
        /// (bedrock, monster spawners, etc.).
        public static final TagKey<Block> PACKING_IGNORED_BLOCKS = createTag("packing_ignored_blocks");

        /// Called by ModTags to initialize static constants.
        protected static void initialize() {}

        /// Registers Block tags with the base game.
        ///
        /// @param name The created tag's identifier.
        /// @return The registered TagKey to be stored.
        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(NomadsCamps.MOD_ID, name));
        }
    }
}
