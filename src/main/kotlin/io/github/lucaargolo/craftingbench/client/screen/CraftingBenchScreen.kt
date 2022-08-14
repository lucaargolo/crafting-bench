package io.github.lucaargolo.craftingbench.client.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler
import io.github.lucaargolo.craftingbench.utils.ModIdentifier
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper

class CraftingBenchScreen(handler: CraftingBenchScreenHandler, inventory: PlayerInventory, title: Text) : HandledScreen<CraftingBenchScreenHandler>(handler, inventory, title) {

    private val heightBtnReference = linkedMapOf<ButtonWidget, Int>()

    private var scrollableOffset = 0.0
    private var scrollable = false
    private var excessHeight = 0.0

    init {
        backgroundWidth = 421
        titleX += 105
        playerInventoryTitleX += 105
    }

    override fun init() {
        super.init()
        clearChildren()
        heightBtnReference.clear()

        scrollableOffset = 0.0
        excessHeight = 0.0
        scrollable = false

        handler.craftableRecipes.entries.forEachIndexed { index, entry ->
            val btn = ButtonWidget(x+5, y+19+(index*20), if(scrollable) 88 else 95, 20, Text.literal("${entry.key.id}"), {
                println("test")
            }, { _, _, _, _ ->

            })
            this.addSelectableChild(btn)
            if(btn.y + btn.height > y+158) {
                if(!scrollable) {
                    scrollable = true
                    children().forEach {
                        (it as? ButtonWidget)?.width = 88
                    }
                    excessHeight += ((btn.y-y) + btn.height) - 158
                }else{
                    excessHeight += btn.height
                }
            }
            heightBtnReference[btn] = btn.y
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        //TODO: If mouse is over button, let you drag it!
        if(scrollable && mouseX in (x+94.0..x+100.0) && mouseY in (y+19.0..y+158.0) && button == 0) {
            scrollableOffset = MathHelper.lerp((mouseY-19-y)/120, 0.0, excessHeight)
            updateButtonsHeight()
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if(scrollable) {
            scrollableOffset -= amount*4
            scrollableOffset = MathHelper.clamp(scrollableOffset, 0.0, excessHeight)
            updateButtonsHeight()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun drawBackground(matrices: MatrixStack, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderTexture(0, TEXTURE)
        drawTexture(matrices, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 512, 256)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title, titleX.toFloat(), titleY.toFloat(), 0xFFFFFF)
        textRenderer.draw(matrices, playerInventoryTitle, playerInventoryTitleX.toFloat(), playerInventoryTitleY.toFloat(), 0xFFFFFF)
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        if(!scrollable) {
            DrawableHelper.fill(matrices, x+5, y+19, x+100, y+158, 0xFF8B8B8B.toInt())
            DrawableHelper.fill(matrices, x+93, y+158, x+94, y+159, 0xFFFFFFFF.toInt())
        }
        children().forEach {
            (it as? ButtonWidget)?.let { btn ->
                if((y..y+155).contains(btn.y)) {
                    btn.render(matrices, mouseX, mouseY, delta)
                }
            }
        }
        RenderSystem.setShaderTexture(0, TEXTURE)
        if(scrollable) {
            val offset = MathHelper.lerp(scrollableOffset / excessHeight, 19.0, 131.0)
            drawTexture(matrices, x + 94, y + offset.toInt(), 0f, 193f, 6, 27, 512, 256)
            drawTexture(matrices, x+4, y, 4f, 0f, 97, 19, 512, 256)
            drawTexture(matrices, x+4, y+158, 4f, 158f, 97, 8, 512, 256)
        }
    }

    private fun updateButtonsHeight() {
        children().forEach {
            (it as? ButtonWidget)?.y = (heightBtnReference[it] ?: 0) - scrollableOffset.toInt()
        }
    }

    companion object {
        private val TEXTURE = ModIdentifier("textures/gui/crafting_bench.png")
    }

}