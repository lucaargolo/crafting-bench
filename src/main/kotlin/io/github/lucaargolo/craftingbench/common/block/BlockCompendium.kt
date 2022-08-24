package io.github.lucaargolo.craftingbench.common.block

import io.github.lucaargolo.craftingbench.CraftingBench
import io.github.lucaargolo.craftingbench.utils.RegistryCompendium
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object BlockCompendium: RegistryCompendium<Block>(Registry.BLOCK) {

    val OAK_CRAFTING_BENCH = register("oak_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))
    val SPRUCE_CRAFTING_BENCH = register("spruce_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))
    val BIRCH_CRAFTING_BENCH = register("birch_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))
    val JUNGLE_CRAFTING_BENCH = register("jungle_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))
    val DARK_OAK_CRAFTING_BENCH = register("dark_oak_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))
    val ACACIA_CRAFTING_BENCH = register("acacia_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))
    val CRIMSON_CRAFTING_BENCH = register("crimson_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))
    val WARPED_CRAFTING_BENCH = register("warped_crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))

    fun registerBlockItems(itemMap: MutableMap<Identifier, Item>) {
        map.forEach { (identifier, block) ->
            itemMap[identifier] = BlockItem(block, CraftingBench.creativeGroupSettings())
        }
    }

    override fun initializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), OAK_CRAFTING_BENCH, SPRUCE_CRAFTING_BENCH, BIRCH_CRAFTING_BENCH, JUNGLE_CRAFTING_BENCH, DARK_OAK_CRAFTING_BENCH, ACACIA_CRAFTING_BENCH, CRIMSON_CRAFTING_BENCH, WARPED_CRAFTING_BENCH)
    }

}