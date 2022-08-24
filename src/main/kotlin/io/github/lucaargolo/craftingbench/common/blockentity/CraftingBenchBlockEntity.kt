package io.github.lucaargolo.craftingbench.common.blockentity

import io.github.lucaargolo.craftingbench.CraftingBench.stacks
import io.github.lucaargolo.craftingbench.common.block.CraftingBenchBlock
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler
import net.minecraft.block.BlockState
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

class CraftingBenchBlockEntity(blockPos: BlockPos, blockState: BlockState) : LockableContainerBlockEntity(BlockEntityCompendium.CRAFTING_BENCH, blockPos, blockState) {

    var craftingInventory = object: SimpleInventory(9) {
        override fun markDirty() {
            this@CraftingBenchBlockEntity.markDirty()
            super.markDirty()
        }
    }
    var inventory = object: SimpleInventory(28) {
        override fun markDirty() {
            this@CraftingBenchBlockEntity.markDirty()
            super.markDirty()
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.put("craftingInventory", Inventories.writeNbt(NbtCompound(), craftingInventory.stacks))
        nbt.put("inventory", Inventories.writeNbt(NbtCompound(), inventory.stacks))
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt.getCompound("craftingInventory"), craftingInventory.stacks)
        Inventories.readNbt(nbt.getCompound("inventory"), inventory.stacks)
    }

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory): ScreenHandler {
        return CraftingBenchScreenHandler(syncId, playerInventory, craftingInventory, inventory, ScreenHandlerContext.create(world, pos))
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean {
        return if (world?.getBlockEntity(pos) != this) {
            false
        } else {
            player.squaredDistanceTo(pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5) <= 64.0
        }
    }

    override fun getContainerName(): Text = CraftingBenchBlock.TITLE

    override fun clear() = inventory.clear()
    override fun size() = inventory.size()
    override fun isEmpty() = inventory.isEmpty
    override fun getStack(slot: Int): ItemStack = inventory.getStack(slot)
    override fun removeStack(slot: Int, amount: Int): ItemStack = inventory.removeStack(slot, amount)
    override fun removeStack(slot: Int): ItemStack = inventory.removeStack(slot)
    override fun setStack(slot: Int, stack: ItemStack?) = inventory.setStack(slot, stack)

}