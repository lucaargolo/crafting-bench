package io.github.lucaargolo.craftingbench.client.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler
import io.github.lucaargolo.craftingbench.utils.ModIdentifier
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class CraftingBenchScreen(handler: CraftingBenchScreenHandler, inventory: PlayerInventory, title: Text) : HandledScreen<CraftingBenchScreenHandler>(handler, inventory, title) {

    private val texture = ModIdentifier("textures/gui/crafting_bench.png")

    init {
        backgroundWidth = 421
        titleX += 105
        playerInventoryTitleX += 105
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
    }

    override fun drawBackground(matrices: MatrixStack, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderTexture(0, texture)
        drawTexture(matrices, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 512, 256)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title, titleX.toFloat(), titleY.toFloat(), 0xFFFFFF)
        textRenderer.draw(matrices, playerInventoryTitle, playerInventoryTitleX.toFloat(), playerInventoryTitleY.toFloat(), 0xFFFFFF)
    }

}