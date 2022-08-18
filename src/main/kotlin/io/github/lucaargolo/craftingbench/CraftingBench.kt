package io.github.lucaargolo.craftingbench

import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.common.blockentity.BlockEntityCompendium
import io.github.lucaargolo.craftingbench.common.item.ItemCompendium
import io.github.lucaargolo.craftingbench.common.screenhandler.ScreenHandlerCompendium
import net.fabricmc.api.ModInitializer

object CraftingBench: ModInitializer {

    const val MOD_ID = "craftingbench"
    override fun onInitialize() {
        BlockCompendium.initialize()
        ItemCompendium.initialize()
        BlockEntityCompendium.initialize()
        ScreenHandlerCompendium.initialize()
    }

}