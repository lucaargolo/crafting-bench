package io.github.lucaargolo.craftingbench.common.screenhandler

import io.github.lucaargolo.craftingbench.client.screen.CraftingBenchScreen
import io.github.lucaargolo.craftingbench.utils.RegistryCompendium
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.registry.Registry

object ScreenHandlerCompendium: RegistryCompendium<ScreenHandlerType<*>>(Registry.SCREEN_HANDLER) {

    val CRAFTING_BENCH = register("crafting_bench", ScreenHandlerType { syncId, playerInventory ->
        CraftingBenchScreenHandler(syncId, playerInventory, ScreenHandlerContext.EMPTY)
    })

    override fun initializeClient() {
        HandledScreens.register(CRAFTING_BENCH, ::CraftingBenchScreen)
    }

}