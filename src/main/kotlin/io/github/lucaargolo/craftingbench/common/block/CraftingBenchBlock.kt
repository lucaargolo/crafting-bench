@file:Suppress("DeprecatedCallableAddReplaceWith")

package io.github.lucaargolo.craftingbench.common.block

import io.github.lucaargolo.craftingbench.common.blockentity.BlockEntityCompendium
import io.github.lucaargolo.craftingbench.common.blockentity.CraftingBenchBlockEntity
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import kotlin.jvm.optionals.getOrNull

class CraftingBenchBlock(settings: Settings) : BlockWithEntity(settings) {

    init {
        defaultState = defaultState.with(FACING, Direction.NORTH).with(TYPE, Type.LEFT)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(FACING)
        builder.add(TYPE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val world = ctx.world
        val pos = ctx.blockPos
        val direction = ctx.playerFacing
        val sideDirection = rotate(direction)

        val canPlaceRight = world.getBlockState(pos.offset(sideDirection)).canReplace(ctx)
        val canPlaceToolbox = world.getBlockState(pos.offset(sideDirection).up()).canReplace(ctx)

        return if(canPlaceRight && canPlaceToolbox) defaultState.with(FACING, direction) else null
    }

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        super.onPlaced(world, pos, state, placer, itemStack)
        if(state[TYPE] == Type.LEFT) {
            val direction = state[FACING]
            val sideDirection = rotate(direction)
            world.setBlockState(pos.offset(sideDirection), state.with(TYPE, Type.RIGHT))
            world.setBlockState(pos.offset(sideDirection).up(), state.with(TYPE, Type.TOOLBOX))
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if(!state.isOf(newState.block)) {
            val direction = state[FACING]
            val sideDirection = rotate(direction)
            when(state[TYPE] ?: Type.LEFT) {
                Type.LEFT -> {
                    world.setBlockState(pos.offset(sideDirection), Blocks.AIR.defaultState)
                    world.setBlockState(pos.offset(sideDirection).up(), Blocks.AIR.defaultState)

                    val blockEntity = world.getBlockEntity(pos)
                    if (blockEntity is Inventory) {
                        ItemScatterer.spawn(world, pos, blockEntity as Inventory?)
                        world.updateComparators(pos, this)
                    }
                }
                Type.RIGHT -> {
                    world.setBlockState(pos.up(), Blocks.AIR.defaultState)
                    world.setBlockState(pos.offset(sideDirection, -1), Blocks.AIR.defaultState)
                }
                Type.TOOLBOX -> {
                    world.setBlockState(pos.down(), Blocks.AIR.defaultState)
                    world.setBlockState(pos.down().offset(sideDirection, -1), Blocks.AIR.defaultState)
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    private fun rotate(dir: Direction): Direction {
        return when(dir) {
            Direction.NORTH -> Direction.EAST
            Direction.EAST -> Direction.SOUTH
            Direction.SOUTH -> Direction.WEST
            else -> Direction.NORTH
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onUse(state: BlockState, world: World, pos: BlockPos?, player: PlayerEntity, hand: Hand?, hit: BlockHitResult?): ActionResult {
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
            val direction = state[FACING]
            val sideDirection = rotate(direction)
            val blockEntityPos = when(state[TYPE] ?: Type.LEFT) {
                Type.LEFT -> pos
                Type.RIGHT -> pos.offset(sideDirection, -1)
                Type.TOOLBOX -> pos.down().offset(sideDirection, -1)
            }
            world.getBlockEntity(blockEntityPos, BlockEntityCompendium.CRAFTING_BENCH).getOrNull()?.let { blockEntity ->
                CraftingBenchScreenHandler(syncId, inventory, blockEntity.craftingInventory, blockEntity.inventory, ScreenHandlerContext.create(world, pos))
            }
        }, TITLE)
    }

    @Deprecated("Deprecated in Java")
    override fun hasComparatorOutput(state: BlockState?): Boolean {
        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Deprecated("Deprecated in Java")
    override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos): Int {
        val direction = state[FACING]
        val sideDirection = rotate(direction)
        val blockEntityPos = when(state[TYPE] ?: Type.LEFT) {
            Type.LEFT -> pos
            Type.RIGHT -> pos.offset(sideDirection, -1)
            Type.TOOLBOX -> pos.down().offset(sideDirection, -1)
        }
        return world.getBlockEntity(blockEntityPos, BlockEntityCompendium.CRAFTING_BENCH).getOrNull()?.let {
            ScreenHandler.calculateComparatorOutput(it as Inventory)
        } ?: 0
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return if(state[TYPE] == Type.LEFT) CraftingBenchBlockEntity(pos, state) else null
    }

    @Deprecated("Deprecated in Java")
    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

    companion object {
        val TITLE: Text = Text.translatable("container.craftingbench.crafting_bench")

        private val FACING = Properties.HORIZONTAL_FACING
        private val TYPE = EnumProperty.of("type", Type::class.java)

        enum class Type: StringIdentifiable {
            LEFT,
            RIGHT,
            TOOLBOX;

            override fun asString() = name.lowercase()
        }
    }

}