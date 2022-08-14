package io.github.lucaargolo.craftingbench.common.block

import io.github.lucaargolo.craftingbench.common.blockentity.BlockEntityCompendium
import io.github.lucaargolo.craftingbench.common.blockentity.CraftingBenchBlockEntity
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.jvm.optionals.getOrNull

class CraftingBenchBlock(settings: Settings) : BlockWithEntity(settings) {

    @Deprecated("Deprecated in Java")
    override fun onUse(state: BlockState, world: World, pos: BlockPos?, player: PlayerEntity, hand: Hand?, hit: BlockHitResult?): ActionResult? {
        return if (world.isClient) {
            ActionResult.SUCCESS
        } else {
            player.openHandledScreen(state.createScreenHandlerFactory(world, pos))
            ActionResult.CONSUME
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Deprecated("Deprecated in Java")
    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory {
        return SimpleNamedScreenHandlerFactory({ syncId, inventory, _ ->
            world.getBlockEntity(pos, BlockEntityCompendium.CRAFTING_BENCH).getOrNull()?.let { blockEntity ->
                CraftingBenchScreenHandler(syncId, inventory, blockEntity.craftingInventory, blockEntity.inventory, ScreenHandlerContext.create(world, pos))
            }
        }, TITLE)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = CraftingBenchBlockEntity(pos, state)

    companion object {
        val TITLE: Text = Text.translatable("craftingbench.container.crafting_bench")
    }

}