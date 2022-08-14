package io.github.lucaargolo.craftingbench.common.blockentity

import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.utils.RegistryCompendium
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.registry.Registry

object BlockEntityCompendium: RegistryCompendium<BlockEntityType<*>>(Registry.BLOCK_ENTITY_TYPE) {

    val CRAFTING_BENCH = register("crafting_bench", BlockEntityType.Builder.create(::CraftingBenchBlockEntity, BlockCompendium.CRAFTING_BENCH).build(null))

}