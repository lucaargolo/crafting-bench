package io.github.lucaargolo.craftingbench.common.blockentity

import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.utils.RegistryCompendium
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.registry.Registry

object BlockEntityCompendium: RegistryCompendium<BlockEntityType<*>>(Registry.BLOCK_ENTITY_TYPE) {

    val CRAFTING_BENCH = register("crafting_bench", BlockEntityType.Builder.create(::CraftingBenchBlockEntity, BlockCompendium.OAK_CRAFTING_BENCH, BlockCompendium.SPRUCE_CRAFTING_BENCH, BlockCompendium.BIRCH_CRAFTING_BENCH, BlockCompendium.JUNGLE_CRAFTING_BENCH, BlockCompendium.DARK_OAK_CRAFTING_BENCH, BlockCompendium.ACACIA_CRAFTING_BENCH, BlockCompendium.CRIMSON_CRAFTING_BENCH, BlockCompendium.WARPED_CRAFTING_BENCH).build(null))

}