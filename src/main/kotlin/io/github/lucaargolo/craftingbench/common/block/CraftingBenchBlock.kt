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
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
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

    @Deprecated("Deprecated in Java")
    override fun getOutlineShape(state: BlockState, world: BlockView?, pos: BlockPos?, context: ShapeContext?): VoxelShape {
        val type = state[TYPE] ?: Type.LEFT
        val direction = state[FACING] ?: Direction.NORTH
        val pair = Pair(type, direction)
        return COMBINED_SHAPES.getOrPut(pair) {
            val sideDirection = rotate(direction)
            var voxelShape = getShapeForState(state)
            when(type) {
                Type.LEFT -> {
                    voxelShape = VoxelShapes.union(voxelShape, getShapeForState(state.with(TYPE, Type.RIGHT)).translate(sideDirection))
                    VoxelShapes.union(voxelShape, getShapeForState(state.with(TYPE, Type.TOOLBOX)).translate(sideDirection).translate(Direction.UP))
                }
                Type.RIGHT -> {
                    voxelShape = VoxelShapes.union(voxelShape, getShapeForState(state.with(TYPE, Type.TOOLBOX)).translate(Direction.UP))
                    VoxelShapes.union(voxelShape, getShapeForState(state.with(TYPE, Type.LEFT)).translate(sideDirection.opposite))
                }
                Type.TOOLBOX -> {
                    voxelShape = VoxelShapes.union(voxelShape, getShapeForState(state.with(TYPE, Type.RIGHT)).translate(Direction.DOWN))
                    VoxelShapes.union(voxelShape, getShapeForState(state.with(TYPE, Type.LEFT)).translate(Direction.DOWN).translate(sideDirection.opposite))
                }
            }
        }
    }

    companion object {
        val TITLE: Text = TranslatableText("container.craftingbench.crafting_bench")

        private val FACING = Properties.HORIZONTAL_FACING
        private val TYPE = EnumProperty.of("type", Type::class.java)

        private val COMBINED_SHAPES: MutableMap<Pair<Type, Direction>, VoxelShape> = mutableMapOf()

        private val LEFT_SHAPE_NORTH: VoxelShape by lazy {
            var shape = VoxelShapes.empty()
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.0625, 1.0, 0.0625, 0.9375, 1.00625, 0.9375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.0, 0.8125, 0.0, 1.0, 1.0, 1.0))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.0125, 0.40625, 0.4375, 1.0, 0.53125, 0.5625))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.0, 0.0, 0.08125, 0.1875, 0.1875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.0, 0.8125, 0.08125, 0.1875, 1.0))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.0625, 0.0625, 0.08125, 0.25, 0.25))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.125, 0.125, 0.08125, 0.3125, 0.3125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.1875, 0.1875, 0.08125, 0.375, 0.375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.25, 0.25, 0.08125, 0.4375, 0.4375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.0625, 0.75, 0.08125, 0.25, 0.9375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.125, 0.6875, 0.08125, 0.3125, 0.875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.1875, 0.625, 0.08125, 0.375, 0.8125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.25, 0.5625, 0.08125, 0.4375, 0.75))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.3125, 0.5, 0.08125, 0.5, 0.6875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.3125, 0.3125, 0.08125, 0.5, 0.5))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.5, 0.5, 0.08125, 0.6875, 0.6875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.5625, 0.5625, 0.08125, 0.75, 0.75))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.625, 0.625, 0.08125, 0.8125, 0.8125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.6875, 0.6875, 0.08125, 0.875, 0.875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.75, 0.75, 0.08125, 0.9375, 0.9375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.5, 0.3125, 0.08125, 0.6875, 0.5))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.5625, 0.25, 0.08125, 0.75, 0.4375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.625, 0.1875, 0.08125, 0.8125, 0.375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.6875, 0.125, 0.08125, 0.875, 0.3125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.01875, 0.75, 0.0625, 0.08125, 0.9375, 0.25))
            shape
        }
        private val LEFT_SHAPE_SOUTH: VoxelShape by lazy {
            LEFT_SHAPE_NORTH.rotate(Direction.SOUTH)
        }
        private val LEFT_SHAPE_EAST: VoxelShape by lazy {
            LEFT_SHAPE_NORTH.rotate(Direction.EAST)
        }
        private val LEFT_SHAPE_WEST: VoxelShape by lazy {
            LEFT_SHAPE_NORTH.rotate(Direction.WEST)
        }

        private val RIGHT_SHAPE_NORTH: VoxelShape by lazy {
            var shape = VoxelShapes.empty()
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.0625, 0.375, 1.0, 0.9375, 1.0, 1.00625))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.0, 0.8125, 0.0, 1.0, 1.0, 1.0))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.0, 0.40625, 0.4375, 0.9875, 0.53125, 0.5625))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.0, 0.0, 0.98125, 0.1875, 0.1875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.0, 0.8125, 0.98125, 0.1875, 1.0))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.0625, 0.75, 0.98125, 0.25, 0.9375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.125, 0.6875, 0.98125, 0.3125, 0.875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.1875, 0.625, 0.98125, 0.375, 0.8125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.25, 0.5625, 0.98125, 0.4375, 0.75))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.3125, 0.5, 0.98125, 0.5, 0.6875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.5, 0.3125, 0.98125, 0.6875, 0.5))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.5625, 0.25, 0.98125, 0.75, 0.4375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.625, 0.1875, 0.98125, 0.8125, 0.375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.6875, 0.125, 0.98125, 0.875, 0.3125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.75, 0.0625, 0.98125, 0.9375, 0.25))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.0625, 0.0625, 0.98125, 0.25, 0.25))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.125, 0.125, 0.98125, 0.3125, 0.3125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.1875, 0.1875, 0.98125, 0.375, 0.375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.25, 0.25, 0.98125, 0.4375, 0.4375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.3125, 0.3125, 0.98125, 0.5, 0.5))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.5, 0.5, 0.98125, 0.6875, 0.6875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.5625, 0.5625, 0.98125, 0.75, 0.75))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.625, 0.625, 0.98125, 0.8125, 0.8125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.6875, 0.6875, 0.98125, 0.875, 0.875))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.91875, 0.75, 0.75, 0.98125, 0.9375, 0.9375))
            shape
        }
        private val RIGHT_SHAPE_SOUTH: VoxelShape by lazy {
            RIGHT_SHAPE_NORTH.rotate(Direction.SOUTH)
        }
        private val RIGHT_SHAPE_EAST: VoxelShape by lazy {
            RIGHT_SHAPE_NORTH.rotate(Direction.EAST)
        }
        private val RIGHT_SHAPE_WEST: VoxelShape by lazy {
            RIGHT_SHAPE_NORTH.rotate(Direction.WEST)
        }

        private val TOOLBOX_SHAPE_NORTH: VoxelShape by lazy {
            var shape = VoxelShapes.empty()
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.125, 0.0, 0.0625, 0.875, 0.375, 0.4375))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.3125, 0.375, 0.1875, 0.375, 0.4375, 0.3125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.3125, 0.4375, 0.1875, 0.6875, 0.5, 0.3125))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.625, 0.375, 0.1875, 0.6875, 0.4375, 0.3125))
            shape
        }
        private val TOOLBOX_SHAPE_SOUTH: VoxelShape by lazy {
            TOOLBOX_SHAPE_NORTH.rotate(Direction.SOUTH)
        }
        private val TOOLBOX_SHAPE_EAST: VoxelShape by lazy {
            TOOLBOX_SHAPE_NORTH.rotate(Direction.EAST)
        }
        private val TOOLBOX_SHAPE_WEST: VoxelShape by lazy {
            TOOLBOX_SHAPE_NORTH.rotate(Direction.WEST)
        }

        private fun getShapeForState(state: BlockState): VoxelShape {
            return when(state[TYPE] ?: Type.LEFT) {
                Type.LEFT -> when(state[FACING]) {
                    Direction.NORTH -> LEFT_SHAPE_NORTH
                    Direction.SOUTH -> LEFT_SHAPE_SOUTH
                    Direction.EAST -> LEFT_SHAPE_EAST
                    Direction.WEST -> LEFT_SHAPE_WEST
                    else -> VoxelShapes.empty()
                }
                Type.RIGHT -> when(state[FACING]) {
                    Direction.NORTH -> RIGHT_SHAPE_NORTH
                    Direction.SOUTH -> RIGHT_SHAPE_SOUTH
                    Direction.EAST -> RIGHT_SHAPE_EAST
                    Direction.WEST -> RIGHT_SHAPE_WEST
                    else -> VoxelShapes.empty()
                }
                Type.TOOLBOX -> when(state[FACING]) {
                    Direction.NORTH -> TOOLBOX_SHAPE_NORTH
                    Direction.SOUTH -> TOOLBOX_SHAPE_SOUTH
                    Direction.EAST -> TOOLBOX_SHAPE_EAST
                    Direction.WEST -> TOOLBOX_SHAPE_WEST
                    else -> VoxelShapes.empty()
                }
            }
        }

        private fun VoxelShape.rotate(to: Direction): VoxelShape = rotate(Direction.NORTH, to)
        private fun VoxelShape.rotate(from: Direction, to: Direction): VoxelShape {
            val buffer = arrayOf(this, VoxelShapes.empty())
            val times: Int = (to.horizontal - from.horizontal + 4) % 4
            for (i in 0 until times) {
                buffer[0].forEachBox { minX, minY, minZ, maxX, maxY, maxZ ->
                    buffer[1] = VoxelShapes.union(buffer[1], VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX))
                }
                buffer[0] = buffer[1]
                buffer[1] = VoxelShapes.empty()
            }
            return buffer[0]
        }

        private fun VoxelShape.translate(direction: Direction): VoxelShape = translate(direction.vector.x + 0.0, direction.vector.y + 0.0, direction.vector.z + 0.0)
        private fun VoxelShape.translate(x: Double, y: Double, z: Double): VoxelShape {
            val buffer = arrayOf(this, VoxelShapes.empty())
            buffer[0].forEachBox { minX, minY, minZ, maxX, maxY, maxZ ->
                buffer[1] = VoxelShapes.union(buffer[1], VoxelShapes.cuboid(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z))
            }
            buffer[0] = buffer[1]
            buffer[1] = VoxelShapes.empty()
            return buffer[0]
        }

        enum class Type: StringIdentifiable {
            LEFT,
            RIGHT,
            TOOLBOX;

            override fun asString() = name.lowercase()
        }
    }

}