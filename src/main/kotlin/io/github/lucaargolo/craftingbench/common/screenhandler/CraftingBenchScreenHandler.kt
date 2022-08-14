package io.github.lucaargolo.craftingbench.common.screenhandler

import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.utils.SimpleCraftingInventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.CraftingResultInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.CraftingResultSlot
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.World

class CraftingBenchScreenHandler(syncId: Int, private val playerInventory: PlayerInventory, simpleCraftingInventory: SimpleInventory, private val inventory: SimpleInventory, private val context: ScreenHandlerContext) : ScreenHandler(ScreenHandlerCompendium.CRAFTING_BENCH, syncId) {

    constructor(syncId: Int, playerInventory: PlayerInventory, context: ScreenHandlerContext): this(syncId, playerInventory, SimpleInventory(9), SimpleInventory(28), context)

    private val craftingInventory = SimpleCraftingInventory(this, 3, 3, simpleCraftingInventory)
    private val result = CraftingResultInventory()

    init {
        addSlot(CraftingResultSlot(playerInventory.player, craftingInventory, result, 0, 283+105, 35))

        repeat(3) { n ->
            repeat(3) { m ->
                addSlot(Slot(craftingInventory, m + n * 3, 189 + 105 + m * 18, 17 + n * 18))
            }
        }

        repeat(3) { n ->
            repeat(9) { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + 105 + m * 18, 84 + n*18))
            }
        }

        repeat(9) { n ->
            addSlot(Slot(playerInventory, n, 8 + 105 + n * 18, 142))
        }

        repeat(4) { n ->
            repeat(7) { m ->
                addSlot(Slot(inventory, m + n * 7, 184 + 105 + m * 18, 84 + n*18))
            }
        }

        onContentChanged(null)
    }

    private fun updateResult(handler: ScreenHandler, world: World, player: PlayerEntity, craftingInventory: CraftingInventory, resultInventory: CraftingResultInventory) {
        if (!world.isClient) {
            val serverPlayerEntity = player as? ServerPlayerEntity
            var itemStack = ItemStack.EMPTY
            val optional = serverPlayerEntity?.server?.recipeManager?.getFirstMatch(RecipeType.CRAFTING, craftingInventory, world)
            if (optional?.isPresent == true) {
                val craftingRecipe = optional.get()
                if (resultInventory.shouldCraftRecipe(world, serverPlayerEntity, craftingRecipe)) {
                    itemStack = craftingRecipe.craft(craftingInventory)
                }
            }
            resultInventory.setStack(0, itemStack)
            handler.setPreviousTrackedSlot(0, itemStack)
            serverPlayerEntity?.networkHandler?.sendPacket(ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack))
        }
    }

    override fun onContentChanged(inventory: Inventory?) {
        context.run { world, _ ->
            updateResult(this, world, playerInventory.player, craftingInventory, result)
        }
    }


    override fun transferSlot(player: PlayerEntity?, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return canUse(context, player, BlockCompendium.CRAFTING_BENCH)
    }

}