package io.github.lucaargolo.craftingbench.client.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler
import io.github.lucaargolo.craftingbench.utils.ModIdentifier
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper

class CraftingBenchScreen(handler: CraftingBenchScreenHandler, inventory: PlayerInventory, title: Text) : HandledScreen<CraftingBenchScreenHandler>(handler, inventory, title) {

    private val buttonRenderFramebuffer = SimpleFramebuffer(1, 1, false, MinecraftClient.IS_SYSTEM_MAC)
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
                handler.hasCraft = true
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
            scrollableOffset = MathHelper.lerp((mouseY-19-y)/139, 0.0, excessHeight)
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
        if(buttonRenderFramebuffer.textureWidth != client?.framebuffer?.textureWidth || buttonRenderFramebuffer.textureHeight != client?.framebuffer?.textureHeight) {
            buttonRenderFramebuffer.resize(client?.framebuffer?.textureWidth ?: 1, client?.framebuffer?.textureHeight ?: 1, MinecraftClient.IS_SYSTEM_MAC)
        }
        buttonRenderFramebuffer.beginWrite(true)
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f)
        RenderSystem.clear(16384, MinecraftClient.IS_SYSTEM_MAC)
        children().forEach {
            (it as? ButtonWidget)?.let { btn ->
                if((y..y+158).contains(btn.y)) {
                    btn.active = true
                    btn.render(matrices, mouseX, mouseY, delta)
                }else{
                    btn.active = false
                }
            }
        }
        buttonRenderFramebuffer.endWrite()
        client?.framebuffer?.beginWrite(true)
        buttonRenderFramebuffer.beginRead()
        RenderSystem.setShaderTexture(0, buttonRenderFramebuffer.colorAttachment)
        val sf = client?.window?.scaleFactor?.toFloat() ?: 1f
        matrices.push()
        matrices.scale(1/sf, 1/sf, 1/sf)

        val matrix = matrices.peek().positionMatrix
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        val bufferBuilder = Tessellator.getInstance().buffer

        val x0 = (x+5f)*sf
        val x1 = ((x+5f)*sf)+(95*sf)
        val y0 = (y+19f)*sf
        val y1 = ((y+19f)*sf)+(139*sf)

        val u0 = ((x+5f)*sf) / buttonRenderFramebuffer.textureWidth
        val u1 = (((x+5f)*sf)+(95*sf)) / buttonRenderFramebuffer.textureWidth
        val v0 = ((y+19f-10.5f)*sf) / buttonRenderFramebuffer.textureHeight
        val v1 = (((y+19f-10.5f)*sf)+(139*sf)) / buttonRenderFramebuffer.textureHeight

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
        bufferBuilder.vertex(matrix, x0, y1, 0f).texture(u0, v0).next()
        bufferBuilder.vertex(matrix, x1, y1, 0f).texture(u1, v0).next()
        bufferBuilder.vertex(matrix, x1, y0, 0f).texture(u1, v1).next()
        bufferBuilder.vertex(matrix, x0, y0, 0f).texture(u0, v1).next()
        BufferRenderer.drawWithShader(bufferBuilder.end())

        matrices.pop()

        buttonRenderFramebuffer.endRead()
        RenderSystem.setShaderTexture(0, TEXTURE)
        if(scrollable) {
            val offset = MathHelper.lerp(scrollableOffset / excessHeight, 19.0, 131.0)
            drawTexture(matrices, x + 94, y + offset.toInt(), 0f, 193f, 6, 27, 512, 256)
        }
        if(handler.hasCraft) {
            if(mouseX in (x+166)..(x+220) && mouseY in (y+38)..(y+65)) {
                drawTexture(matrices, x + 166, y + 38, 421f, 54f, 54, 27, 512, 256)
            }else{
                drawTexture(matrices, x + 166, y + 38, 421f, 27f, 54, 27, 512, 256)
            }
        }else{
            drawTexture(matrices, x + 166, y + 38, 421f, 0f, 54, 27, 512, 256)
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