package io.github.lucaargolo.craftingbench.common.block

import io.github.lucaargolo.craftingbench.utils.RegistryCompendium
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayers
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object BlockCompendium: RegistryCompendium<Block>(Registry.BLOCK) {

    val CRAFTING_BENCH = register("crafting_bench", CraftingBenchBlock(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).nonOpaque()))

    fun registerBlockItems(itemMap: MutableMap<Identifier, Item>) {
        map.forEach { (identifier, block) ->
            itemMap[identifier] = BlockItem(block, Item.Settings().group(ItemGroup.DECORATIONS))
        }
    }

    override fun initializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), CRAFTING_BENCH)
    }

}