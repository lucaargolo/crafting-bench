package io.github.lucaargolo.craftingbench

import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.common.blockentity.BlockEntityCompendium
import io.github.lucaargolo.craftingbench.common.item.ItemCompendium
import io.github.lucaargolo.craftingbench.common.screenhandler.ScreenHandlerCompendium
import io.github.lucaargolo.craftingbench.mixin.SimpleInventoryAccessor
import io.github.lucaargolo.craftingbench.utils.ModIdentifier
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Rarity
import net.minecraft.util.collection.DefaultedList

object CraftingBench: ModInitializer {

    const val MOD_ID = "craftingbench"
    private val creativeTab = FabricItemGroupBuilder.create(ModIdentifier("creative_tab")).icon{ ItemStack(BlockCompendium.OAK_CRAFTING_BENCH) }.build()

    override fun onInitialize() {
        BlockCompendium.initialize()
        ItemCompendium.initialize()
        BlockEntityCompendium.initialize()
        ScreenHandlerCompendium.initialize()
    }

    fun creativeGroupSettings(): Item.Settings = Item.Settings().group(creativeTab).rarity(Rarity.UNCOMMON)

    val SimpleInventory.stacks: DefaultedList<ItemStack>
        get() = (this as SimpleInventoryAccessor).stacks

}